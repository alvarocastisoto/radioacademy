import { Component, inject, OnInit, ChangeDetectorRef } from '@angular/core';
import { AdminService } from '../../services/admin/admin';
import { CourseService } from '../../services/course/course';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import Swal from 'sweetalert2';
@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-users.html',
  styleUrl: './admin-users.scss',
})
export class AdminUsersComponent implements OnInit {
  
  private adminService = inject(AdminService);
  private cdr = inject(ChangeDetectorRef);

  
  users: any[] = []; 
  filteredUsers: any[] = []; 
  courses: any[] = []; 
  userCourses: any[] = []; 

  
  selectedUser: any = null;
  searchTerm: string = ''; 
  selectedCourseIdToAdd: any = null; 

  
  availableRoles = ['STUDENT', 'ADMIN', 'TEACHER'];
  roleLabels: any = {
    STUDENT: 'Alumno',
    USER: 'Alumno', 
    ADMIN: 'Administrador',
    TEACHER: 'Profesor',
  };

  ngOnInit() {
    this.loadData();
  }

  
  
  
  loadData() {
    
    this.adminService.getUsers().subscribe({
      next: (data) => {
        console.log('✅ Usuarios cargados:', data);
        this.users = data;
        this.filteredUsers = data; 
        this.cdr.detectChanges(); 
      },
      error: (e) => console.error('Error cargando usuarios:', e),
    });

    
    this.adminService.getCoursesLight().subscribe({
      next: (data) => {
        this.courses = data;
        this.cdr.detectChanges();
      },
    });
  }

  
  
  
  filterUsers() {
    const term = this.searchTerm.toLowerCase().trim();

    
    if (!term) {
      this.filteredUsers = this.users;
    } else {
      
      this.filteredUsers = this.users.filter(
        (user) =>
          (user.fullName && user.fullName.toLowerCase().includes(term)) ||
          (user.email && user.email.toLowerCase().includes(term)) ||
          (user.dni && user.dni.toLowerCase().includes(term))
      );
    }
  }

  
  
  
  selectUser(user: any) {
    this.selectedUser = user;
    this.selectedCourseIdToAdd = null; 
    this.loadUserCourses(user.id); 
  }

  loadUserCourses(userId: string) {
    this.adminService.getUserCourses(userId).subscribe({
      next: (data) => {
        this.userCourses = data;
        this.cdr.detectChanges();
      },
      error: () => {
        this.userCourses = []; 
      },
    });
  }

  
  
  
  addCourse() {
    if (!this.selectedUser || !this.selectedCourseIdToAdd) return;

    this.adminService.enrollUser(this.selectedUser.id, this.selectedCourseIdToAdd).subscribe({
      next: () => {
        this.loadUserCourses(this.selectedUser.id);
        this.selectedCourseIdToAdd = null;

        Swal.fire({
          title: '¡Matriculado!',
          text: `El usuario ahora tiene acceso al curso.`,
          icon: 'success',
          confirmButtonColor: '#4f46e5',
          timer: 2000,
          timerProgressBar: true,
        });
      },
      error: (err) => {
        console.error('Error matriculando:', err);

        
        const msg = err.error?.message || 'Error desconocido al matricular.';

        Swal.fire({
          title: 'No se pudo matricular',
          text: msg, 
          icon: 'error',
          confirmButtonColor: '#4f46e5',
        });
      },
    });
  }

  removeCourse(courseId: any) {
    Swal.fire({
      title: '¿Revocar acceso?',
      text: 'El usuario perderá el acceso a este curso inmediatamente.',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#ef4444',
      cancelButtonColor: '#94a3b8',
      confirmButtonText: 'Sí, eliminar',
      cancelButtonText: 'Cancelar',
      reverseButtons: true,
    }).then((result) => {
      if (result.isConfirmed) {
        this.adminService.unenrollUser(this.selectedUser.id, courseId).subscribe({
          next: () => {
            this.loadUserCourses(this.selectedUser.id);

            const Toast = Swal.mixin({
              toast: true,
              position: 'top-end',
              showConfirmButton: false,
              timer: 3000,
            });
            Toast.fire({ icon: 'success', title: 'Acceso eliminado correctamente' });
          },
          error: (err) => {
            
            const msg = err.error?.message || 'No se pudo eliminar el curso.';
            Swal.fire('Error', msg, 'error');
          },
        });
      }
    });
  }

  
  
  
  updateRole(newRole: string) {
    if (!this.selectedUser) return;

    const roleName = this.roleLabels[newRole] || newRole;

    
    Swal.fire({
      title: '¿Cambiar Rol?',
      html: `Vas a cambiar el rol de <b>${this.selectedUser.fullName}</b> a <br><span class="badge bg-primary fs-5 mt-2">${roleName}</span>`,
      icon: 'question',
      showCancelButton: true,
      confirmButtonColor: '#4f46e5',
      cancelButtonColor: '#94a3b8',
      confirmButtonText: 'Sí, cambiar rol',
    }).then((result) => {
      if (result.isConfirmed) {
        this.adminService.changeUserRole(this.selectedUser.id, newRole).subscribe({
          next: () => {
            this.selectedUser.role = newRole;
            const userInList = this.users.find((u) => u.id === this.selectedUser.id);
            if (userInList) userInList.role = newRole;
            this.cdr.detectChanges();

            Swal.fire({
              title: '¡Actualizado!',
              text: 'El rol ha sido modificado con éxito.',
              icon: 'success',
              confirmButtonColor: '#4f46e5',
            });
          },
          error: () => Swal.fire('Error', 'No se pudo cambiar el rol', 'error'),
        });
      } else {
        
        
        const oldRole = this.selectedUser.role;
        this.selectedUser.role = null;
        this.cdr.detectChanges();
        this.selectedUser.role = oldRole;
      }
    });
  }

  
  
  
  getInitials(fullName: string): string {
    if (!fullName) return '';
    const parts = fullName.trim().split(' ');
    if (parts.length === 1) return parts[0].substring(0, 2).toUpperCase();
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  }

  getAvatarColor(id: string): string {
    let sum = 0;
    if (!id) return 'bg-avatar-0';
    for (let i = 0; i < id.length; i++) {
      sum += id.charCodeAt(i);
    }
    return `bg-avatar-${sum % 4}`;
  }
}
