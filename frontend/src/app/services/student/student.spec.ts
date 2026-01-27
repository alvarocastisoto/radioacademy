import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { StudentService } from './student';

describe('StudentService', () => {
  let service: StudentService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        StudentService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(StudentService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should get my courses', () => {
    const mockCourses = [{ id: '1', title: 'Course 1' }];
    service.getMyCourses().subscribe(courses => {
      expect(courses).toEqual(mockCourses as any);
    });

    const req = httpMock.expectOne(`${(service as any).apiUrl}/student/dashboard`);
    expect(req.request.method).toBe('GET');
    req.flush(mockCourses);
  });
});
