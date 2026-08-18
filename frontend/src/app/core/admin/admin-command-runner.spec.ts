import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { beforeEach, describe, expect, it } from 'vitest';

import { Translation } from '@core/i18n/translation';

import { AdminActionState, IDLE_ACTION } from './admin-action.model';
import { AdminCommandRunner } from './admin-command-runner';

describe('AdminCommandRunner', () => {
  let runner: AdminCommandRunner;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        {
          provide: Translation,
          // Returns the key itself: these tests only care that the fallback key was asked for, not
          // what a real dictionary would render it as.
          useValue: { translate: (key: string) => key },
        },
      ],
    });

    runner = TestBed.inject(AdminCommandRunner);
  });

  it('reports running while the command is in flight, then done with the success message', async () => {
    const state = signal<AdminActionState>(IDLE_ACTION);
    let resolveCommand!: (value: string) => void;
    const command = new Promise<string>((resolve) => {
      resolveCommand = resolve;
    });

    const run = runner.run(() => command, {
      state,
      successMessage: (result) => `done: ${result}`,
    });

    expect(state()).toEqual({ status: 'running', message: '' });

    resolveCommand('ok');
    await run;

    expect(state()).toEqual({ status: 'done', message: 'done: ok' });
  });

  it('reports the translated fallback message when the command rejects with a plain error', async () => {
    const state = signal<AdminActionState>(IDLE_ACTION);

    await runner.run(() => Promise.reject(new Error('network down')), {
      state,
      successMessage: () => 'unreachable on failure',
    });

    expect(state()).toEqual({ status: 'error', message: 'admin.actionFailed' });
  });

  it('surfaces the backend detail when the command rejects with an HTTP problem response', async () => {
    const state = signal<AdminActionState>(IDLE_ACTION);
    const failure = new HttpErrorResponse({
      status: 409,
      error: { detail: 'a synchronization is already in progress' },
    });

    await runner.run(() => Promise.reject(failure), {
      state,
      successMessage: () => 'unreachable on failure',
    });

    expect(state()).toEqual({
      status: 'error',
      message: 'a synchronization is already in progress',
    });
  });

  it('toggles the optional busy flag on for the duration of the command and off afterwards', async () => {
    const state = signal<AdminActionState>(IDLE_ACTION);
    const busy = signal(false);
    let resolveCommand!: () => void;
    const command = new Promise<void>((resolve) => {
      resolveCommand = resolve;
    });

    const run = runner.run(() => command, { state, busy, successMessage: () => 'done' });

    expect(busy()).toBe(true);

    resolveCommand();
    await run;

    expect(busy()).toBe(false);
  });

  it('turns the busy flag back off even when the command fails', async () => {
    const state = signal<AdminActionState>(IDLE_ACTION);
    const busy = signal(false);

    await runner.run(() => Promise.reject(new Error('boom')), {
      state,
      busy,
      successMessage: () => 'unreachable on failure',
    });

    expect(busy()).toBe(false);
  });

  it('runs onSuccess with the command result after a success, and never after a failure', async () => {
    const state = signal<AdminActionState>(IDLE_ACTION);
    let sideEffectResult: string | null = null;

    await runner.run(() => Promise.resolve('created-id'), {
      state,
      successMessage: () => 'done',
      onSuccess: (result) => {
        sideEffectResult = result;
      },
    });

    expect(sideEffectResult).toBe('created-id');

    let onSuccessCalledAfterFailure = false;
    await runner.run(() => Promise.reject(new Error('boom')), {
      state,
      successMessage: () => 'unreachable on failure',
      onSuccess: () => {
        onSuccessCalledAfterFailure = true;
      },
    });

    expect(onSuccessCalledAfterFailure).toBe(false);
  });
});
