import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Login } from './login';
import { AuthService } from '../../services/auth/auth';
import { Router, provideRouter } from '@angular/router';
import { ReactiveFormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';

describe('Login', () => {
  let component: any; // Using any because of the name ambiguity
  let fixture: ComponentFixture<any>;
  let authServiceSpy: any;
  let router: Router;

  beforeEach(async () => {
    authServiceSpy = { login: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [Login, ReactiveFormsModule],
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        provideRouter([])
      ]
    })
      .compileComponents();

    // Re-importing Login because it is standalone
    TestBed.overrideComponent(Login, {
      add: { imports: [ReactiveFormsModule] }
    });

    fixture = TestBed.createComponent(Login);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate');
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should call login on valid submit', () => {
    authServiceSpy.login.mockReturnValue(of({ token: '123' }));
    component.loginForm.setValue({ email: 'test@test.com', password: 'password' });
    component.onSubmit();

    expect(authServiceSpy.login).toHaveBeenCalledWith({ email: 'test@test.com', password: 'password' });
    expect(router.navigate).toHaveBeenCalledWith(['/courses']);
  });

  it('should not call login on invalid submit', () => {
    component.loginForm.setValue({ email: 'invalid', password: '' });
    component.onSubmit();

    expect(authServiceSpy.login).not.toHaveBeenCalled();
  });
});
