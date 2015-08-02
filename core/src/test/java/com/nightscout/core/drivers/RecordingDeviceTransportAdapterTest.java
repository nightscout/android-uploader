package com.nightscout.core.drivers;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RecordingDeviceTransportAdapterTest {

  @Mock
  private DeviceTransport mockTransport;

  private RecordingDeviceTransportAdapter transportAdapter;

  @Before
  public void setUp() {
    transportAdapter = new RecordingDeviceTransportAdapter(mockTransport);
  }

  @Test
  public void shouldRecordOpen() throws Exception {
    transportAdapter.open();
    verify(mockTransport, times(1)).open();
    assertThat(transportAdapter.getActions().size(), is(1));
    assertThat(transportAdapter.getActions().get(0).getAction(), is(DeviceInteraction.Type.OPEN));
  }

  @Test
  public void shouldRecordClose() throws Exception {
    transportAdapter.close();
    verify(mockTransport, times(1)).close();
    assertThat(transportAdapter.getActions().size(), is(1));
    assertThat(transportAdapter.getActions().get(0).getAction(), is(DeviceInteraction.Type.CLOSE));
  }

  @Test
  public void shouldRecordRead() throws Exception {
    byte[] out = {0, 1, 2};
    when(mockTransport.read(1, 2)).thenReturn(out);
    transportAdapter.read(1, 2);
    verify(mockTransport, times(1)).read(1, 2);
    assertThat(transportAdapter.getActions().size(), is(1));
    DeviceInteraction action = transportAdapter.getActions().get(0);
    assertThat(action.getAction(), is(DeviceInteraction.Type.READ));
    assertThat(action.getByteArray(), is(out));
    assertThat(action.getByteLength(), is(1));
  }

  @Test
  public void shouldRecordWrite() throws Exception {
    byte[] out = {0, 1, 2};
    when(mockTransport.write(out, 1)).thenReturn(3);
    transportAdapter.write(out, 1);
    verify(mockTransport, times(1)).write(out, 1);
    assertThat(transportAdapter.getActions().size(), is(1));
    DeviceInteraction action = transportAdapter.getActions().get(0);
    assertThat(action.getAction(), is(DeviceInteraction.Type.WRITE));
    assertThat(action.getByteArray(), is(out));
    assertThat(action.getByteLength(), is(3));
  }

  @Test
  public void shouldRecordMultipleActions() throws Exception {
    transportAdapter.open();
    transportAdapter.close();
    verify(mockTransport, times(1)).open();
    verify(mockTransport, times(1)).close();
    assertThat(transportAdapter.getActions().size(), is(2));
    assertThat(transportAdapter.getActions().get(0).getAction(), is(DeviceInteraction.Type.OPEN));
    assertThat(transportAdapter.getActions().get(1).getAction(), is(DeviceInteraction.Type.CLOSE));
  }

  @Test
  public void shouldClearActions() throws Exception {
    transportAdapter.open();
    transportAdapter.close();
    verify(mockTransport, times(1)).open();
    verify(mockTransport, times(1)).close();
    assertThat(transportAdapter.getActions().size(), is(2));
    transportAdapter.clearActions();
    assertThat(transportAdapter.getActions().size(), is(0));
  }
}
