import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { authGuard } from './auth-guard';
import { AuthService } from '../services/auth/auth';
import { signal } from '@angular/core';
import { describe, it, expect, beforeEach, vi } from 'vitest';

describe('authGuard', () => {
  let authServiceSpy: any;
  let routerSpy: any;

  beforeEach(() => {
    authServiceSpy = { currentUser: signal<any>(null) };
    routerSpy = { navigate: vi.fn() };

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy }
      ]
    });
  });

  const executeGuard = (route: any = {}, state: any = {}) =>
    TestBed.runInInjectionContext(() => authGuard(route, state));

  it('should allow access if user is logged in', () => {
    authServiceSpy.currentUser.set({ id: '1', email: 'test@test.com' });
    const result = executeGuard();
    expect(result).toBe(true);
  });

  it('should redirect to login if user is not logged in', () => {
    authServiceSpy.currentUser.set(null);
    const result = executeGuard();
    expect(result).toBe(false);
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/login']);
  });
});
