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
  // Inyecciones de dependencias
  private adminService = inject(AdminService);
  private cdr = inject(ChangeDetectorRef);

  // Datos principales
  users: any[] = []; // Lista completa original
  filteredUsers: any[] = []; // Lista filtrada (la que se ve en la tabla)
  courses: any[] = []; // Lista de todos los cursos (para el desplegable)
  userCourses: any[] = []; // Cursos del usuario seleccionado (panel lateral)

  // Estado de la interfaz
  selectedUser: any = null;
  searchTerm: string = ''; // Texto del buscador
  selectedCourseIdToAdd: any = null; // ID del curso a añadir (UUID es string)

  // Configuración de Roles
  availableRoles = ['STUDENT', 'ADMIN', 'TEACHER'];
  roleLabels: any = {
    STUDENT: 'Alumno',
    USER: 'Alumno', // Por si acaso
    ADMIN: 'Administrador',
    TEACHER: 'Profesor',
  };

  ngOnInit() {
    this.loadData();
  }

  // ==========================================
  // 1. CARGA DE DATOS INICIAL
  // ==========================================
  loadData() {
    // A. Cargar Usuarios
    this.adminService.getUsers().subscribe({
      next: (data) => {
        console.log('✅ Usuarios cargados:', data);
        this.users = data;
        this.filteredUsers = data; // Al inicio, mostramos todos
        this.cdr.detectChanges(); // Forzar repintado
      },
      error: (e) => console.error('Error cargando usuarios:', e),
    });

    // B. Cargar Cursos (versión ligera para el select)
    this.adminService.getCoursesLight().subscribe({
      next: (data) => {
        this.courses = data;
        this.cdr.detectChanges();
      },
    });
  }

  // ==========================================
  // 2. LÓGICA DEL BUSCADOR
  // ==========================================
  filterUsers() {
    const term = this.searchTerm.toLowerCase().trim();

    // Si no hay texto, mostramos todos
    if (!term) {
      this.filteredUsers = this.users;
    } else {
      // Filtramos por Nombre, Email o DNI
      this.filteredUsers = this.users.filter(
        (user) =>
          (user.fullName && user.fullName.toLowerCase().includes(term)) ||
          (user.email && user.email.toLowerCase().includes(term)) ||
          (user.dni && user.dni.toLowerCase().includes(term))
      );
    }
  }

  // ==========================================
  // 3. SELECCIÓN DE USUARIO
  // ==========================================
  selectUser(user: any) {
    this.selectedUser = user;
    this.selectedCourseIdToAdd = null; // Limpiar selector de curso
    this.loadUserCourses(user.id); // Cargar sus cursos específicos
  }

  loadUserCourses(userId: string) {
    this.adminService.getUserCourses(userId).subscribe({
      next: (data) => {
        this.userCourses = data;
        this.cdr.detectChanges();
      },
      error: () => {
        this.userCourses = []; // Si falla, lista vacía
      },
    });
  }

  // ==========================================
  // 4. GESTIÓN DE MATRÍCULAS
  // ==========================================
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

        // 👇 AQUÍ ESTÁ LA CLAVE: Leemos el mensaje del backend
        const msg = err.error?.message || 'Error desconocido al matricular.';

        Swal.fire({
          title: 'No se pudo matricular',
          text: msg, // 👈 Mostramos "Este usuario YA está matriculado..."
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
            // 👇 También aquí leemos el error real
            const msg = err.error?.message || 'No se pudo eliminar el curso.';
            Swal.fire('Error', msg, 'error');
          },
        });
      }
    });
  }

  // ==========================================
  // 5. GESTIÓN DE ROLES
  // ==========================================
  updateRole(newRole: string) {
    if (!this.selectedUser) return;

    const roleName = this.roleLabels[newRole] || newRole;

    // 🔥 CONFIRMACIÓN DE CAMBIO DE ROL
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
        // Si cancela, forzamos un repintado para que el Select vuelva a su sitio visualmente
        // (truco sucio pero efectivo para que no parezca que cambió)
        const oldRole = this.selectedUser.role;
        this.selectedUser.role = null;
        this.cdr.detectChanges();
        this.selectedUser.role = oldRole;
      }
    });
  }

  // ==========================================
  // 6. UTILIDADES VISUALES (AVATAR)
  // ==========================================
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
