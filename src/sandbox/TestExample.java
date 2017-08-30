package sandbox;

import org.junit.Test;

public class TestExample
{

  @Test
  public void test() throws InterruptedException
  {

    Example lExample = new Example();

    lExample.open();

    for (int i = 0; i < 1000; i++)

    {
      System.out.println(i);
      lExample.setButtonText("Button " + i);
      Thread.sleep(1000);
    }

  }

}
