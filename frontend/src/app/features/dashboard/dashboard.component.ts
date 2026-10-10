import { Component, ElementRef, OnInit, ViewChild, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService, LmsUser, UserRole } from '../../core/api.service';

type Role = 'STUDENT' | 'INSTRUCTOR' | 'ADMIN';

@Component({selector:'app-dashboard',standalone:true,imports:[CommonModule,FormsModule],templateUrl:'./dashboard-workspace.component.html',styleUrls:['./dashboard.component.css','./dashboard-workspace.component.css','./lesson-progress.css']})
export class DashboardComponent implements OnInit {
  readonly api = inject(ApiService);
  @ViewChild('courseSearch') private courseSearch?: ElementRef<HTMLInputElement>;

  active = 'Overview';
  courses: any[] = [];
  users: any[] = [];
  enrollments: any[] = [];
  activity: any[] = [];
  analytics: any = {};
  settings: any = { platformName: 'GU_LMS', timezone: 'Asia/Calcutta', allowSelfEnrollment: true };
  assignments: any[] = [];
  submissions: any[] = [];
  selectedCourse: any;
  selectedAssignment: any;
  search = '';
  toast = '';
  error = '';
  editingUser: any = null;
  showUserForm = false;
  showCourseForm = false;
  showAssignmentForm = false;
  showHelp = false;
  showNotifications = false;
  profileForm = { name: '', email: '' };
  newUser: any = { name: '', email: '', password: 'welcome', role: 'STUDENT' };
  newCourse: any = { title: '', description: '', syllabus: '', category: 'Development', level: 'Beginner' };
  newAssignment: any = { title: '', description: '', points: 100 };
  submissionText = '';
  grade = 0;
  feedback = '';
  selectedLearningEnrollment: any = null;
  learningLessons: any[] = [];

  get currentUser(): LmsUser { return this.api.user()!; }
  get role(): UserRole { return this.currentUser.role; }
  get isAdmin(): boolean { return this.role === 'ADMIN'; }
  get isInstructor(): boolean { return this.role === 'INSTRUCTOR'; }
  get isStudent(): boolean { return this.role === 'STUDENT'; }
  get filteredCourses(): any[] {
    const query = this.search.trim().toLocaleLowerCase();
    return this.courses.filter((course) => (course.title || '').toLocaleLowerCase().includes(query));
  }
  get monthlyEnrollmentMaximum(): number {
    return Math.max(0, ...(this.analytics.monthlyEnrollments || []).map((month: any) => month.count));
  }
  enrollmentBarHeight(count: number): number {
    return this.monthlyEnrollmentMaximum ? Math.max(3, count / this.monthlyEnrollmentMaximum * 100) : 0;
  }

  ngOnInit(): void { this.load(); }

  load(): void {
    this.api.get<any[]>('/courses').subscribe({ next: (value) => this.courses = value, error: (error) => this.fail(error) });
    const analyticsPath = this.isInstructor ? `/analytics/overview?instructorId=${this.currentUser.id}` : '/analytics/overview';
    if (!this.isStudent) this.api.get<any>(analyticsPath).subscribe({ next: (value) => this.analytics = value, error: (error) => this.fail(error) });
    this.api.get<any[]>('/activity').subscribe({ next: (value) => this.activity = value, error: (error) => this.fail(error) });
    if (this.isAdmin) this.api.get<any[]>('/users').subscribe({ next: (value) => this.users = value, error: (error) => this.fail(error) });
    if (this.isStudent) {
      this.api.get<any[]>(`/students/${this.currentUser.id}/enrollments`).subscribe({ next: (value) => this.enrollments = value, error: (error) => this.fail(error) });
    } else {
      this.enrollments = [];
    }
    if (this.isAdmin) this.api.get<any>('/settings').subscribe({ next: (value) => this.settings = value, error: (error) => this.fail(error) });
  }

  navigate(page: string): void {
    this.active = page;
    if (page === 'Assignments' && !this.selectedCourse && this.courses.length) this.selectCourse(this.courses[0]);
  }

  focusCourseSearch(): void {
    this.active = 'Courses';
    setTimeout(() => this.courseSearch?.nativeElement.focus());
  }

  toggleNotifications(): void {
    this.showNotifications = !this.showNotifications;
    if (this.showNotifications) {
      this.api.get<any[]>('/activity').subscribe({
        next: (value) => this.activity = value,
        error: (error) => this.fail(error)
      });
    }
  }

  viewAllActivity(): void {
    this.showNotifications = false;
    this.navigate('Activity');
  }

  openProfile(): void {
    this.profileForm = { name: this.currentUser.name, email: this.currentUser.email };
    this.navigate('Profile');
  }

  saveProfile(): void {
    this.api.updateProfile(this.currentUser.id, this.profileForm).subscribe({
      next: (user) => { this.api.user.set(user); this.notice('Profile updated'); },
      error: (error) => this.fail(error)
    });
  }

  logout(): void {
    this.api.logout();
    window.location.assign('/auth');
  }

  enroll(course: any): void {
    this.api.post('/enrollments', { studentId: this.currentUser.id, courseId: course.id }).subscribe({
      next: () => { this.notice('Successfully enrolled'); this.load(); },
      error: (error) => this.fail(error)
    });
  }

  selectCourse(course: any): void {
    this.selectedCourse = course;
    this.selectedAssignment = null;
    this.assignments = [];
    if (course) this.api.get<any[]>(`/courses/${course.id}/assignments`).subscribe({
      next: (value) => this.assignments = value,
      error: (error) => this.fail(error)
    });
  }

  createCourse(): void {
    this.api.post('/courses', { ...this.newCourse, instructorId: this.currentUser.id, instructorName: this.currentUser.name }).subscribe({
      next: () => { this.notice('Course created successfully'); this.showCourseForm = false; this.load(); },
      error: (error) => this.fail(error)
    });
  }

  updateCourse(course: any): void {
    this.api.put(`/courses/${course.id}`, course).subscribe({ next: () => this.notice('Course updated'), error: (error) => this.fail(error) });
  }

  deleteCourse(course: any): void {
    if (!confirm(`Delete ${course.title}?`)) return;
    this.api.delete(`/courses/${course.id}`).subscribe({
      next: () => { this.notice('Course deleted'); this.load(); },
      error: (error) => this.fail(error)
    });
  }

  saveUser(): void {
    const request = this.editingUser
      ? this.api.put(`/users/${this.editingUser.id}`, this.newUser)
      : this.api.post('/users', this.newUser);
    request.subscribe({
      next: () => { this.notice(this.editingUser ? 'User updated' : 'User created'); this.closeUserForm(); this.load(); },
      error: (error) => this.fail(error)
    });
  }

  editUser(user: any): void {
    this.editingUser = user;
    this.newUser = { ...user, password: '' };
    this.showUserForm = true;
  }

  closeUserForm(): void {
    this.showUserForm = false;
    this.editingUser = null;
    this.newUser = { name: '', email: '', password: 'welcome', role: 'STUDENT' };
  }

  removeUser(id: number): void {
    if (!confirm('Delete this user?')) return;
    this.api.delete(`/users/${id}`).subscribe({ next: () => { this.notice('User deleted'); this.load(); }, error: (error) => this.fail(error) });
  }

  createAssignment(): void {
    if (!this.selectedCourse) return;
    this.api.post('/assignments', { ...this.newAssignment, courseId: this.selectedCourse.id }).subscribe({
      next: () => { this.notice('Assignment created'); this.showAssignmentForm = false; this.selectCourse(this.selectedCourse); },
      error: (error) => this.fail(error)
    });
  }

  viewSubmissions(assignment: any): void {
    this.selectedAssignment = assignment;
    this.api.get<any[]>(`/assignments/${assignment.id}/submissions`).subscribe({ next: (value) => this.submissions = value, error: (error) => this.fail(error) });
  }

  submitAssignment(): void {
    if (!this.selectedAssignment || !this.submissionText.trim()) return;
    this.api.post('/submissions', {
      assignmentId: this.selectedAssignment.id,
      studentId: this.currentUser.id,
      studentName: this.currentUser.name,
      content: this.submissionText.trim()
    }).subscribe({
      next: () => { this.notice('Assignment submitted'); this.submissionText = ''; this.load(); },
      error: (error) => this.fail(error)
    });
  }

  gradeSubmission(submission: any): void {
    this.api.patch(`/submissions/${submission.id}/grade`, { grade: this.grade, feedback: this.feedback }).subscribe({
      next: () => { this.notice('Grade saved'); this.viewSubmissions(this.selectedAssignment); },
      error: (error) => this.fail(error)
    });
  }

  updateProgress(enrollment: any, progress: number): void {
    this.api.patch(`/enrollments/${enrollment.id}/progress`, { progress }).subscribe({
      next: () => { this.notice('Progress updated'); this.load(); },
      error: (error) => this.fail(error)
    });
  }

  openLearningCourse(enrollment: any): void {
    this.selectedLearningEnrollment = enrollment;
    this.api.get<any[]>(`/students/${this.currentUser.id}/courses/${enrollment.courseId}/lessons`).subscribe({
      next: (value) => this.learningLessons = value,
      error: (error) => this.fail(error)
    });
  }

  completeLesson(lesson: any): void {
    this.api.post<any>(`/lessons/${lesson.id}/complete`, {}).subscribe({
      next: (result) => {
        lesson.completed = true;
        if (this.selectedLearningEnrollment) {
          this.selectedLearningEnrollment.progress = result.progress;
          this.selectedLearningEnrollment.status = result.status;
        }
        const enrollment = this.enrollments.find((item) => item.id === this.selectedLearningEnrollment?.id);
        if (enrollment) { enrollment.progress = result.progress; enrollment.status = result.status; }
        this.notice('Lesson completed');
      },
      error: (error) => this.fail(error)
    });
  }

  saveSettings(): void {
    this.api.put('/settings', this.settings).subscribe({
      next: (settings) => { this.settings = settings; this.notice('Settings updated successfully'); },
      error: (error) => this.fail(error)
    });
  }

  notice(message: string): void {
    this.error = '';
    this.toast = message;
    setTimeout(() => this.toast = '', 3000);
  }

  fail(error: any): void {
    this.error = error?.error?.detail || error?.error?.message || error?.message || 'Unable to complete the request';
  }
}
