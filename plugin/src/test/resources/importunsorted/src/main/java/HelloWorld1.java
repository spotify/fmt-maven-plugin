package notestsource.src.main.java;

import static com.google.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import javax.annotations.Nullable;
import com.google.common.base.Preconditions;
import java.util.List;

public class HelloWorld1 {

  <T> void check(@Nullable List<T> x) {
    Preconditions.checkNodeNull(x);
  }

  void f() {
    List<String> xs = null;
    assertThat(xs).isNull();
    try {
      check(xs);
      fail();
    } catch (NullPointerException e) {
    }
  }
}
