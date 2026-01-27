import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { AuthService } from './auth';
import { describe, it, expect, beforeEach, vi } from 'vitest';

describe('AuthService', () => {
  let service: AuthService;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AuthService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Router, useValue: { navigate: vi.fn() } }
      ]
    });
    service = TestBed.inject(AuthService);
    router = TestBed.inject(Router);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should login and store token', () => {
    const mockResponse = { token: 'fake-token', user: { id: '1', email: 'test@test.com', name: 'Test', surname: 'User', role: 'STUDENT' as const } };
    // @ts-ignore - access private method for testing purpose or use public methods
    service['handleAuthSuccess'](mockResponse);

    expect(localStorage.getItem('token')).toBe('fake-token');
    expect(service.currentUser()).toEqual(mockResponse.user);
  });

  it('should logout and clear storage', () => {
    localStorage.setItem('token', 'fake-token');
    service.logout();

    expect(localStorage.getItem('token')).toBeNull();
    expect(service.currentUser()).toBeNull();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });
});
