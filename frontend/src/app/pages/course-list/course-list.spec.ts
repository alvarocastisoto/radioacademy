import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CourseList } from './course-list';
import { CourseService } from '../../services/course/course';
import { PaymentService } from '../../services/payment/payment';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';

describe('CourseList', () => {
  let component: CourseList;
  let fixture: ComponentFixture<CourseList>;
  let courseServiceSpy: any;
  let paymentServiceSpy: any;

  beforeEach(async () => {
    courseServiceSpy = { getCourses: vi.fn().mockReturnValue(of([])) };
    paymentServiceSpy = { buyCourse: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [CourseList],
      providers: [
        { provide: CourseService, useValue: courseServiceSpy },
        { provide: PaymentService, useValue: paymentServiceSpy },
        provideRouter([])
      ]
    })
      .compileComponents();

    fixture = TestBed.createComponent(CourseList);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
