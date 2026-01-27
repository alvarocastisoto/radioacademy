import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CourseDetails } from './course-details';
import { CourseService } from '../../services/course/course';
import { MediaService } from '../../services/media/media';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';

describe('CourseDetails', () => {
  let component: CourseDetails;
  let fixture: ComponentFixture<CourseDetails>;
  let courseServiceSpy: any;
  let mediaServiceSpy: any;

  beforeEach(async () => {
    courseServiceSpy = {
      getCourseModules: vi.fn().mockReturnValue(of([])),
      getModuleLessons: vi.fn().mockReturnValue(of([]))
    };
    mediaServiceSpy = { getLessonPdfBlob: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [CourseDetails],
      providers: [
        { provide: CourseService, useValue: courseServiceSpy },
        { provide: MediaService, useValue: mediaServiceSpy },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { paramMap: { get: () => '123' } }
          }
        },
        provideRouter([])
      ]
    })
      .compileComponents();

    fixture = TestBed.createComponent(CourseDetails);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
