import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ProfileComponent } from './profile';
import { StudentService } from '../../services/student/student';
import { AuthService } from '../../services/auth/auth';
import { MediaService } from '../../services/media/media';
import { ReactiveFormsModule } from '@angular/forms';
import { of } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';

describe('ProfileComponent', () => {
  let component: ProfileComponent;
  let fixture: ComponentFixture<ProfileComponent>;
  let studentServiceSpy: any;
  let authServiceSpy: any;
  let mediaServiceSpy: any;

  beforeEach(async () => {
    studentServiceSpy = {
      getProfile: vi.fn().mockReturnValue(of({ name: '', surname: '', email: '' })),
      updateProfile: vi.fn()
    };
    authServiceSpy = { updateUserFields: vi.fn(), logout: vi.fn() };
    mediaServiceSpy = { toPublicUrl: vi.fn().mockReturnValue(''), uploadFile: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [ProfileComponent, ReactiveFormsModule],
      providers: [
        { provide: StudentService, useValue: studentServiceSpy },
        { provide: AuthService, useValue: authServiceSpy },
        { provide: MediaService, useValue: mediaServiceSpy }
      ]
    })
      .compileComponents();

    fixture = TestBed.createComponent(ProfileComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
