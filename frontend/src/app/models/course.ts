export interface Course {
  id: string;
  title: string;
  description: string;
  coverImage: string;
  price: number;
  hours: number;
  isPurchased: boolean; // 👈 CRÍTICO: Debe llamarse igual que en Java
}
