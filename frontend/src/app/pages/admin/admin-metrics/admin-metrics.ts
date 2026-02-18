import { Component, AfterViewInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { forkJoin, Subscription } from 'rxjs';
import { AdminService } from '../../../services/admin/admin';
import Chart from 'chart.js/auto';
import type { ChartConfiguration, ScatterDataPoint, Chart as ChartJS } from 'chart.js';
import 'chartjs-adapter-date-fns';

type DailyMetric = { day: string; value: number };
type TopCourse = { courseId: string; enrollments: number; revenue: number };

@Component({
  selector: 'app-admin-metrics',
  templateUrl: './admin-metrics.html',
  styleUrls: ['./admin-metrics.scss'],
})
export class AdminMetricsComponent implements AfterViewInit, OnDestroy {
  private sub?: Subscription;
  private charts: ChartJS<'line', ScatterDataPoint[], unknown>[] = [];

  topCoursesData: TopCourse[] = [];
  loading = true;

  constructor(private metrics: AdminService, private cdr: ChangeDetectorRef ) {}

  ngAfterViewInit(): void {
    console.log('Iniciando petición de métricas al backend...');

    
    this.sub = forkJoin({
      registered: this.metrics.registeredDaily(),
      enrollments: this.metrics.enrollmentsDaily(),
      revenue: this.metrics.revenueDaily(),
      topCourses: this.metrics.topCourses(),
    }).subscribe({
      next: (res) => {
        console.log('Datos recibidos correctamente:', res);
        this.loading = false;
        this.cdr.detectChanges();

        
        
        setTimeout(() => {
          this.processAndRender(res);
        }, 50);
      },
      error: (err) => {
        this.loading = false;
        this.cdr.detectChanges();
        console.error('Error en la comunicación con el backend:', err);
        
      },
    });
  }

private processAndRender(res: any): void {
  try {
    
    const registered: ScatterDataPoint[] = (res.registered || []).map((d: any) => ({
      x: new Date(d.day).getTime(),
      y: Number(d.count ?? 0), 
    }));

    
    const enrollments: ScatterDataPoint[] = (res.enrollments || []).map((d: any) => ({
      x: new Date(d.day).getTime(),
      y: Number(d.count ?? d.value ?? 0),
    }));

    
    const revenue: ScatterDataPoint[] = (res.revenue || []).map((d: any) => ({
      x: new Date(d.day).getTime(),
      y: Number(d.revenue ?? d.value ?? d.count ?? 0),
    }));

    
    
    this.renderLineChart('registeredChart', 'Usuarios registrados / día', registered, '#3b82f6');
    this.renderLineChart('enrollmentsChart', 'Matrículas / día', enrollments, '#f59e0b');
    this.renderLineChart('revenueChart', 'Ingresos / día', revenue, '#10b981');

    console.log('Gráficos renderizados con datos reales.');
  } catch (e) {
    console.error('Error procesando los datos:', e);
  }
}

  private renderLineChart(
    canvasId: string,
    label: string,
    data: ScatterDataPoint[],
    color: string,
  ) {
    const canvas = document.getElementById(canvasId) as HTMLCanvasElement | null;

    if (!canvas) {
      console.warn(`No se encontró el elemento canvas con id: ${canvasId}`);
      return;
    }

    this.destroyChartById(canvasId);

    const config: ChartConfiguration<'line', ScatterDataPoint[], unknown> = {
      type: 'line',
      data: {
        datasets: [
          {
            label,
            data,
            parsing: false,
            tension: 0.3,
            borderColor: color,
            backgroundColor: color + '33', 
            fill: true,
            pointRadius: 4,
            pointHoverRadius: 6,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          x: {
            type: 'time',
            time: {
              unit: 'day',
              displayFormats: { day: 'dd MMM' },
            },
            grid: { display: false },
          },
          y: {
            beginAtZero: true,
            ticks: { precision: 0 },
          },
        },
        plugins: {
          legend: { position: 'top' as const },
        },
      },
    };

    const chart = new Chart(canvas, config);
    (chart as any).__canvasId = canvasId;
    this.charts.push(chart);
  }

  ngOnDestroy(): void {
    if (this.sub) {
      this.sub.unsubscribe();
    }
    this.destroyCharts();
  }

  private destroyCharts() {
    this.charts.forEach((c) => c.destroy());
    this.charts = [];
  }

  private destroyChartById(canvasId: string) {
    const idx = this.charts.findIndex((c) => (c as any).__canvasId === canvasId);
    if (idx >= 0) {
      this.charts[idx].destroy();
      this.charts.splice(idx, 1);
    }
  }

  formatEuro(v: number): string {
    return new Intl.NumberFormat('es-ES', { style: 'currency', currency: 'EUR' }).format(v);
  }
}
