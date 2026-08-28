import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { App } from './App';

describe('App shell', () => {
  it('renders the app title', () => {
    render(<App />);
    expect(screen.getByRole('heading', { name: 'SwasthyaVaani' })).toBeInTheDocument();
  });

  it('shows a connectivity status', () => {
    render(<App />);
    expect(screen.getByRole('status')).toBeInTheDocument();
  });
});
