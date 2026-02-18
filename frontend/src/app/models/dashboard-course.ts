export interface DashboardCourse {
  id: string;
  title: string;
  description: string;
  coverImage: string | null; 
  pdfUrl: string | null;
  progress: number;
}
