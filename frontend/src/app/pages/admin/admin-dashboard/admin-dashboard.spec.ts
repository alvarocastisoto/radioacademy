import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AdminDashboard } from './admin-dashboard';
import { CourseService } from '../../../services/course/course';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';

describe('AdminDashboard', () => {
  let component: AdminDashboard;
  let fixture: ComponentFixture<AdminDashboard>;
  let courseServiceSpy: any;

  beforeEach(async () => {
    courseServiceSpy = { getCourses: vi.fn().mockReturnValue(of([])) };

    await TestBed.configureTestingModule({
      imports: [AdminDashboard],
      providers: [
        { provide: CourseService, useValue: courseServiceSpy },
        provideRouter([])
      ]
    })
      .compileComponents();

    fixture = TestBed.createComponent(AdminDashboard);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
