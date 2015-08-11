package com.nightscout.android;

import org.robolectric.TestLifecycleApplication;

import java.lang.reflect.Method;

public class TestNightscout extends Nightscout implements TestLifecycleApplication {

  @Override
  public void beforeTest(Method method) {
    System.out.println("before!");
  }

  @Override
  public void prepareTest(Object test) {
    System.out.println("prepare!");

  }

  @Override
  public void afterTest(Method method) {
    System.out.println("after!");

  }
}
