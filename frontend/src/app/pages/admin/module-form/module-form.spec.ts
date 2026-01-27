import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ModuleForm } from './module-form';
import { CourseService } from '../../../services/course/course';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { ReactiveFormsModule } from '@angular/forms';
import { of } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';

describe('ModuleForm', () => {
  let component: ModuleForm;
  let fixture: ComponentFixture<ModuleForm>;
  let courseServiceSpy: any;

  beforeEach(async () => {
    courseServiceSpy = { createModule: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [ModuleForm, ReactiveFormsModule],
      providers: [
        { provide: CourseService, useValue: courseServiceSpy },
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

    fixture = TestBed.createComponent(ModuleForm);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
