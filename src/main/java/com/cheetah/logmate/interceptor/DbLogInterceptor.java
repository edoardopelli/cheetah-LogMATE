package com.cheetah.logmate.interceptor;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.data.annotation.Id;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.cheetah.logmate.dto.ColumnState;
import com.cheetah.logmate.dto.DbAudit;
import com.cheetah.logmate.dto.DbCache;
import com.cheetah.logmate.dto.ExecCondition;
import com.cheetah.logmate.dto.QueryType;
import com.cheetah.logmate.repositories.DbLogRepository;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.extern.slf4j.Slf4j;

@Component
@Aspect
@Slf4j
@Conditional(value = ExecCondition.class)
public class DbLogInterceptor {

	@Autowired
	private DbLogRepository repository;
	

	@Pointcut("execution(* jakarta.persistence.EntityManager+.persist(..))")
	public void entityManagerPersistPointcut() {
	}

	@Pointcut("execution(* jakarta.persistence.EntityManager+.merge(..))")
	public void entityManagerMergePointcut() {
	}

	@Pointcut("execution(* jakarta.persistence.EntityManager+.find(..))")
	public void entityManagerFindPointcut() {
	}

	@Pointcut("execution(* jakarta.persistence.EntityManager+.remove(..))")
	public void entityManagerRemovePointcut() {
	}

	private void cacheOldValues(Object currentRecord)
			throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		Map<String, Object> entityOldValuesMap = new HashMap<>();

		Field[] fields = null;
		fields = currentRecord.getClass().getDeclaredFields();
		setCurrentValuesOnMap(entityOldValuesMap, currentRecord, fields);
		DbCache.setCurrentEntitySelectedValues(entityOldValuesMap);
		System.out.println(entityOldValuesMap);
	}

	@Around("entityManagerPersistPointcut() || entityManagerMergePointcut() || entityManagerFindPointcut() || entityManagerRemovePointcut() ")
	@Transactional
	public Object aroundEntityManager(ProceedingJoinPoint jp) throws Throwable {

		String methodName = jp.getSignature().getName();

		if ("persist".equals(methodName)) {
			Object entity = jp.getArgs()[0];
			doAction(entity);
		}

		Object result = null;
		try {
			result = jp.proceed(jp.getArgs());
		} catch (Throwable e) {
			repository.delete(DbCache.getCurrentAudit());
		}
		if (result != null) {
			switch (methodName) {
			case "find":
				cacheOldValues(result);
				break;
			case "merge":
				doAction(result);
				break;
			case "remove":
				Map<String, Object> entityOldValues = DbCache.getCurrentEntitySelectedValues();
				DbAudit.DbAuditBuilder tableBuilder = DbAudit.builder();
				String tableName = jp.getArgs()[0].getClass().getAnnotation(Table.class).name();
				tableBuilder.tableName(tableName);
				tableBuilder.dateTime(LocalDateTime.now()).queryType(QueryType.DELETE);
				tableBuilder.userId("FAKE_USER");
				Set<Entry<String, Object>> keys = entityOldValues.entrySet();
				ColumnState.ColumnStateBuilder builder = ColumnState.builder();
				List<ColumnState> changes = new ArrayList<>();
				DbAudit audit = tableBuilder.changes(changes).build();
				for (Entry<String, Object> key : keys) {
					Object currentValue = null;
					Object previousValue = entityOldValues != null ? entityOldValues.get(key.getKey()) : null;
					boolean isChanged = false;
					if ((currentValue == null && previousValue != null)
							|| (currentValue != null && previousValue == null)) {
						isChanged = true;
					}
					if (currentValue != null && previousValue != null) {
						isChanged = !currentValue.equals(previousValue);
					}
					builder.columnName(key.getKey().toString()).changed(isChanged).currentValue(currentValue)
							.previousValue(previousValue).build();
					ColumnState cc = builder.build();
					System.out.println(cc);
					audit.getChanges().add(cc);

				}

				audit = repository.insert(audit);
				break;
			}

		}
		return result;

	}

	private void doAction(Object arg) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		Table table = arg.getClass().getAnnotation(Table.class);
		if (table != null) {
			String tableName = table.name();
			Field[] fields = arg.getClass().getDeclaredFields();
			Map<String, Object> entityNewValuesMap = new HashMap<>();
			boolean isUpdate = setCurrentValuesOnMap(entityNewValuesMap, arg, fields);

			Map<String, Object> entityOldValuesMap = DbCache.getCurrentEntitySelectedValues();
			if (!entityNewValuesMap.equals(entityOldValuesMap)) {
				ColumnState.ColumnStateBuilder builder = ColumnState.builder();
				DbAudit.DbAuditBuilder tableBuilder = DbAudit.builder();
				tableBuilder.dateTime(LocalDateTime.now()).tableName(tableName);
				List<ColumnState> changes = new ArrayList<>();
				for (Field field : fields) {
					Column col = field.getAnnotation(Column.class);
					if (col != null) {
						Object currentValue = entityNewValuesMap.get(col.name());
						Object previousValue = entityOldValuesMap != null ? entityOldValuesMap.get(col.name()) : null;
						boolean isChanged = false;
						if ((currentValue == null && previousValue != null)
								|| (currentValue != null && previousValue == null)) {
							isChanged = true;
						}
						if (currentValue != null && previousValue != null) {
							isChanged = !currentValue.equals(previousValue);
						}
						builder.columnName(col.name()).changed(isChanged).currentValue(currentValue)
								.previousValue(previousValue);
						if (isUpdate) {
							tableBuilder.queryType(QueryType.UPDATE);
						}

						if (!isUpdate) {
							tableBuilder.queryType(QueryType.INSERT);
						}
						ColumnState cc = builder.build();
						changes.add(cc);
					}
				}
				DbAudit audit = tableBuilder.changes(changes).userId("FAKE_USER").build();
				audit = repository.save(audit);
				DbCache.setCurrentAudit(audit);
				// qui
				for (ColumnState cc : changes) {
					System.out.println(cc);
				}
			}
		}
	}

	private boolean setCurrentValuesOnMap(Map<String, Object> valuesMap, Object currentRecord, Field[] fields)
			throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		boolean isUpdate = false;
		for (Field f : fields) {
			if (f.getAnnotation(OneToMany.class) != null) {
				continue;
			}
			Field embeddedId = null;
			if (f.getAnnotation(EmbeddedId.class) != null) {
				embeddedId = f;
			}

			Column col = f.getAnnotation(Column.class);
			if (col != null) {
				Id id = f.getAnnotation(Id.class);
				if (id != null) {
					Object pk = currentRecord.getClass().getMethod(createMetodNameGet(f)).invoke(currentRecord);
					isUpdate = pk != null;
				}
				valuesMap.put(col.name(),
						currentRecord.getClass().getMethod(createMetodNameGet(f)).invoke(currentRecord));
			}
			if (embeddedId != null) {
				Object pk = currentRecord.getClass().getMethod(createMetodNameGet(f)).invoke(currentRecord);
				isUpdate = pk != null;
				if (isUpdate) {
					Field[] pkFs = pk.getClass().getDeclaredFields();
					for (Field pkF : pkFs) {
						Column colPk = pkF.getAnnotation(Column.class);
						if (colPk != null) {
							valuesMap.put(colPk.name(), pk.getClass().getMethod(createMetodNameGet(pkF)).invoke(pk));
						}

					}
				}
			}
		}
		return isUpdate;
	}

	private String createMetodNameGet(Field id) {
		return "get" + StringUtils.capitalize(id.getName());
	}
}
