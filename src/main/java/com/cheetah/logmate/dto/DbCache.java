package com.cheetah.logmate.dto;

import java.util.Map;

public class DbCache {
	
	private static ThreadLocal<Map<String,Object>> currentEntitySelectedValues = new ThreadLocal<>();
	
	/**
	 * viene memorizzato l'oggetto audit in fase di inserimento, per consentire il rollback su mongo db in caso di errore di inserimento su db2
	 */
	private static ThreadLocal<DbAudit> currentAudit = new ThreadLocal<>();
	
	
	public static Map<String, Object> getCurrentEntitySelectedValues() {
		return currentEntitySelectedValues.get();
	}

	public static void setCurrentEntitySelectedValues(Map<String,Object> currentEntitySelected) {
		currentEntitySelectedValues.set(currentEntitySelected);
	}
	
	public static DbAudit getCurrentAudit() {
		return currentAudit.get();
	}
	
	public static void setCurrentAudit(DbAudit audit) {
		currentAudit.set(audit);
	}

}
