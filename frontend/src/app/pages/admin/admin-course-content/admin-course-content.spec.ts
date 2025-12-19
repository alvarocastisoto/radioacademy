import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AdminCourseContent } from './admin-course-content';

describe('AdminCourseContent', () => {
  let component: AdminCourseContent;
  let fixture: ComponentFixture<AdminCourseContent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminCourseContent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AdminCourseContent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
