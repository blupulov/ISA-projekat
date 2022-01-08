package com.example.backend.Repository;

import com.example.backend.Beans.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

}