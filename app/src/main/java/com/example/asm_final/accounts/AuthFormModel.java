package com.example.asm_final.accounts;

import androidx.lifecycle.*;
import com.example.asm_final.R;
import java.util.concurrent.*;

/** Retains drafts and an in-flight request across theme/language recreation, never on disk. */
public final class AuthFormModel extends ViewModel {
  public String identifier = "", name = "", email = "", password = "", confirm = "";
  public boolean remember, accepted, restoreAttempted;

  public static final class State {
    public final boolean busy;
    public final int error;
    public final AuthTicket ticket;

    State(boolean b, int e, AuthTicket t) {
      busy = b;
      error = e;
      ticket = t;
    }
  }

  public final MutableLiveData<State> state = new MutableLiveData<>(new State(false, 0, null));

  public void submit(Executor executor, Callable<AuthTicket> task, int rejected) {
    State current = state.getValue();
    if (current != null && current.busy) return;
    state.setValue(new State(true, 0, null));
    executor.execute(
        () -> {
          try {
            AuthTicket ticket = task.call();
            state.postValue(new State(false, ticket == null ? rejected : 0, ticket));
          } catch (Exception error) {
            int message = R.string.auth_storage_error;
            if (error instanceof com.example.asm_final.backend.BackendClient.Failure) {
              String code = ((com.example.asm_final.backend.BackendClient.Failure) error).code;
              message =
                  "INVALID_CREDENTIALS".equals(code)
                      ? R.string.login_msg_fail
                      : "ACCOUNT_ALREADY_EXISTS".equals(code)
                          ? R.string.auth_account_exists
                          : "RATE_LIMITED".equals(code)
                              ? R.string.auth_server_rate_limit
                              : R.string.auth_server_error;
            } else if (error instanceof java.io.IOException) message = R.string.auth_server_offline;
            state.postValue(new State(false, message, null));
          }
        });
  }

  public void consumed() {
    password = "";
    confirm = "";
    state.setValue(new State(false, 0, null));
  }

  @Override
  protected void onCleared() {
    password = "";
    confirm = "";
  }
}
