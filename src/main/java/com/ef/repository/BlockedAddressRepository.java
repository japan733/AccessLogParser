package com.ef.repository;

import com.ef.entity.AccessLog;
import com.ef.entity.BlockedAddress;
import org.springframework.data.repository.CrudRepository;

public interface BlockedAddressRepository extends CrudRepository<BlockedAddress, Integer> {

}
