package com.example.backend.Controllers;

import com.example.backend.Beans.Grade;
import com.example.backend.Dtos.GradeToSaveDto;
import com.example.backend.Dtos.GradeToShowDto;
import com.example.backend.Services.GradeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@CrossOrigin(allowedHeaders = "*")
@RestController
@RequestMapping("grade")
public class GradeController {
    @Autowired
    private final GradeService gradeService;

    public GradeController(GradeService gradeService){
        this.gradeService = gradeService;
    }

    @GetMapping()
    public ResponseEntity<Collection<Grade>> getAllGrades(){
        return new ResponseEntity<>(gradeService.getAllGrades(), HttpStatus.OK);
    }

    @GetMapping(path = "/instructor/{id}")
    public ResponseEntity<GradeToShowDto> getAllGradesOfInstructor(@PathVariable long id){
        return new ResponseEntity<>(gradeService.getAllGradesOfInstructor(id), HttpStatus.OK);
    }

    @GetMapping(path = "/revision")
    public ResponseEntity<Collection<Grade>> getAllGradesForRevision(){
        return new ResponseEntity<>(gradeService.getAllGradesForRevision(), HttpStatus.OK);
    }

    @PostMapping()
    public ResponseEntity<Grade> saveGrade(@RequestBody GradeToSaveDto dto){
        return new ResponseEntity<>(gradeService.makeGradeForSaving(dto), HttpStatus.CREATED);
    }

    @DeleteMapping(path = "/{id}")
    public ResponseEntity<?> deleteGrade(@PathVariable long id){
        gradeService.deleteGrade(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PutMapping(path = "/enable/{id}")
    public ResponseEntity<?> enableGrade(@PathVariable long id){
        gradeService.enableGrade(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
