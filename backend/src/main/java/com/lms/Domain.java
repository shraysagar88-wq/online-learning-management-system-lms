package com.lms;

import jakarta.persistence.*;
import java.time.LocalDateTime;

enum Role { ADMIN, INSTRUCTOR, STUDENT }

@Entity @Table(name="users")
class User {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    String name; @Column(unique=true) String email; String password;
    @Enumerated(EnumType.STRING) Role role;
    boolean active = true; LocalDateTime createdAt = LocalDateTime.now();
    public User() {}
    User(String name,String email,String password,Role role){this.name=name;this.email=email;this.password=password;this.role=role;}
    public Long getId(){return id;} public String getName(){return name;} public String getEmail(){return email;} public String getPassword(){return password;} public void setPassword(String v){password=v;}
    public Role getRole(){return role;} public boolean isActive(){return active;} public void setName(String v){name=v;} public void setEmail(String v){email=v;}
    public void setRole(Role v){role=v;} public void setActive(boolean v){active=v;}
}

@Entity
class Course {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    String title; @Column(length=4000) String description; String syllabus; String category; String level;
    String thumbnail; boolean published = true; Long instructorId; String instructorName;
    LocalDateTime createdAt = LocalDateTime.now();
    public Course() {}
    Course(String title,String description,String syllabus,String category,String level,Long instructorId,String instructorName){this.title=title;this.description=description;this.syllabus=syllabus;this.category=category;this.level=level;this.instructorId=instructorId;this.instructorName=instructorName;}
    public Long getId(){return id;} public String getTitle(){return title;} public String getDescription(){return description;} public String getSyllabus(){return syllabus;}
    public String getCategory(){return category;} public String getLevel(){return level;} public String getThumbnail(){return thumbnail;} public boolean isPublished(){return published;}
    public Long getInstructorId(){return instructorId;} public String getInstructorName(){return instructorName;} public void setTitle(String v){title=v;} public void setDescription(String v){description=v;}
    public void setSyllabus(String v){syllabus=v;} public void setCategory(String v){category=v;} public void setLevel(String v){level=v;} public void setPublished(boolean v){published=v;}
}

@Entity @Table(uniqueConstraints=@UniqueConstraint(columnNames={"studentId","courseId"}))
class Enrollment {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id; Long studentId; Long courseId; String courseTitle; int progress; String status="IN_PROGRESS"; LocalDateTime enrolledAt=LocalDateTime.now();
    public Enrollment(){} Enrollment(Long s,Long c,String t){studentId=s;courseId=c;courseTitle=t;}
    public Long getId(){return id;} public Long getStudentId(){return studentId;} public Long getCourseId(){return courseId;} public String getCourseTitle(){return courseTitle;} public int getProgress(){return progress;} public String getStatus(){return status;} public LocalDateTime getEnrolledAt(){return enrolledAt;} public void setProgress(int v){progress=v; status=v>=100?"COMPLETED":"IN_PROGRESS";}
}

@Entity
class Assignment { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id; Long courseId; String title; String description; LocalDateTime dueDate; int points=100; public Assignment(){} public Assignment(Long c,String t,String d,int p){courseId=c;title=t;description=d;points=p;} public Long getId(){return id;} public Long getCourseId(){return courseId;} public String getTitle(){return title;} public String getDescription(){return description;} public LocalDateTime getDueDate(){return dueDate;} public int getPoints(){return points;} public void setDueDate(LocalDateTime value){dueDate=value;} }

@Entity
class Submission { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id; Long assignmentId; Long studentId; String studentName; String content; Integer grade; String feedback; LocalDateTime submittedAt=LocalDateTime.now(); public Submission(){} public Submission(Long a,Long s,String n,String c){assignmentId=a;studentId=s;studentName=n;content=c;} public Long getId(){return id;} public Long getAssignmentId(){return assignmentId;} public Long getStudentId(){return studentId;} public String getStudentName(){return studentName;} public String getContent(){return content;} public Integer getGrade(){return grade;} public String getFeedback(){return feedback;} public void grade(Integer g,String f){grade=g;feedback=f;} }

@Entity @Table(name="platform_settings")
class PlatformSettings {
    @Id Long id=1L;
    String platformName="GU_LMS";
    boolean allowSelfEnrollment=true;
    boolean maintenanceMode;
    String timezone="Asia/Calcutta";
    public PlatformSettings(){}
    public Long getId(){return id;} public String getPlatformName(){return platformName;} public boolean isAllowSelfEnrollment(){return allowSelfEnrollment;}
    public boolean isMaintenanceMode(){return maintenanceMode;} public String getTimezone(){return timezone;}
    public void setPlatformName(String value){platformName=value;} public void setAllowSelfEnrollment(boolean value){allowSelfEnrollment=value;}
    public void setMaintenanceMode(boolean value){maintenanceMode=value;} public void setTimezone(String value){timezone=value;}
}
