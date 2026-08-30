import { ComponentFixture, TestBed } from '@angular/core/testing';
import { KpiCardComponent } from './kpi-card.component';

describe('KpiCardComponent', () => {
  let component: KpiCardComponent;
  let fixture: ComponentFixture<KpiCardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [KpiCardComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(KpiCardComponent);
    component = fixture.componentInstance;
  });

  it('should create the component instance', () => {
    expect(component).toBeTruthy();
  });

  it('should render label and formatted currency value', () => {
    component.label = 'Total Revenue';
    component.value = '$39,990.00';
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.card-label')?.textContent).toContain('Total Revenue');
    expect(compiled.querySelector('.card-value')?.textContent).toContain('$39,990.00');
  });
});
