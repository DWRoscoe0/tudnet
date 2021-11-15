package allClasses;

import java.util.EventListener;

public interface ShutdownerListener extends EventListener

  /* This interface is used by the AppInstanceManager class.  */

  {
    public void doMyShutdown();
    }
