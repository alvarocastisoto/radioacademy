import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CoursePlayer } from './course-player';

describe('CoursePlayer', () => {
  let component: CoursePlayer;
  let fixture: ComponentFixture<CoursePlayer>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CoursePlayer]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CoursePlayer);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
