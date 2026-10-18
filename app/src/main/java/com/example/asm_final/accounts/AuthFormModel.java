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
            state.postValue(new State(false, R.string.auth_storage_error, null));
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
