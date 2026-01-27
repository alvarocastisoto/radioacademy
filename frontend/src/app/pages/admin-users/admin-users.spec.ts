import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AdminUsersComponent } from './admin-users';
import { AdminService } from '../../services/admin/admin';
import { of } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { describe, it, expect, beforeEach, vi } from 'vitest';

describe('AdminUsersComponent', () => {
  let component: AdminUsersComponent;
  let fixture: ComponentFixture<AdminUsersComponent>;
  let adminServiceSpy: any;

  beforeEach(async () => {
    adminServiceSpy = {
      getUsers: vi.fn().mockReturnValue(of([])),
      getCoursesLight: vi.fn().mockReturnValue(of([])),
      getUserCourses: vi.fn().mockReturnValue(of([]))
    };

    await TestBed.configureTestingModule({
      imports: [AdminUsersComponent, FormsModule],
      providers: [
        { provide: AdminService, useValue: adminServiceSpy }
      ]
    })
      .compileComponents();

    fixture = TestBed.createComponent(AdminUsersComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
