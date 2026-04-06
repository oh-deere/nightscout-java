package se.ohdeere.nightscout.storage.devicestatus;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

public interface DeviceStatusRepository extends CrudRepository<DeviceStatus, UUID> {

	@Query("SELECT * FROM device_status ORDER BY created_at DESC LIMIT :count")
	List<DeviceStatus> findLatest(int count);

	@Query("SELECT * FROM device_status ORDER BY created_at DESC LIMIT 1")
	DeviceStatus findCurrent();

}
