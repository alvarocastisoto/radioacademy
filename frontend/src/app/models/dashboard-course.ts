export interface DashboardCourse {
  id: string;
  title: string;
  description: string;
  coverImage: string | null; // La URL absoluta que viene del backend
  pdfUrl: string | null;
  progress: number;
}
