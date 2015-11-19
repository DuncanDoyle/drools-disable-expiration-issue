package org.jboss.ddoyle.drools.model;

import java.util.Date;
import java.util.UUID;

public class SimpleEvent {
	
	
	private long timestamp;
	
	private final long logicalTimestamp;
	
	private final String id;
	
	public SimpleEvent() {
		this(UUID.randomUUID().toString());
	}
	
	public SimpleEvent(final String id) {
		this.id = id;
		long currentTime = new Date().getTime();
		this.timestamp = currentTime;
		this.logicalTimestamp = currentTime;
	}
	
	public SimpleEvent(final long timestamp) {
		this(UUID.randomUUID().toString(), timestamp, timestamp);
	}
	
	public SimpleEvent(final long timestamp, final long logicalTimestamp) {
		this(UUID.randomUUID().toString(), timestamp, logicalTimestamp);
	}
	
	public SimpleEvent(final String id, final long timestamp) {
		this(id, timestamp, timestamp);
	}
	
	public SimpleEvent(final String id, final long timestamp, final long logicalTimestamp) {
		this.id = id;
		this.timestamp = timestamp;
		this.logicalTimestamp = logicalTimestamp;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public String getId() {
		return id;
	}

	public long getLogicalTimestamp() {
		return logicalTimestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	

}
