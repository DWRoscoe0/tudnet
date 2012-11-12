package allClasses;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;

public class DirectoryTableCellRenderer
  extends javax.swing.table.DefaultTableCellRenderer
  /* This small helper class extends the method getTableCellRendererComponent()
    used for rendering DirectoryTableViewer (JTable) cells.
    It could be renamed to something more generic, for plain JTable cells,
    since the rendering is not directory-specific.  ???
    */
  { // class DirectoryTableCellRenderer
	  private static final long serialVersionUID = 1L;

	  public Component getTableCellRendererComponent
      ( JTable table, 
        Object value,
        boolean isSelected, 
        boolean hasFocus, 
        int row, 
        int column
        )
      { // getTableCellRendererComponent(.)

        /* stuff having to do with the type icon.
        if  // override text with icon if value is an Icon.
          ( value != null && value instanceof Icon )  // process Icon value,
            {
              super.getTableCellRendererComponent( 
                table, value, isSelected, hasFocus, row, column );
              setIcon( (Icon)value );
              setText( "" );
              return this;
              }
          else   // process non-Icon value,
            {
            setIcon( null );
            }

        return super.getTableCellRendererComponent(  // use super-class to finish job.
          table, value, isSelected, hasFocus, row, column );
        */
        // if (column > 0)
        Component RenderComponent= super.getTableCellRendererComponent( 
          table, value, isSelected, hasFocus, row, column );

        Color FocusDependentSelectionColor;
          if (table.isFocusOwner())  // and base color on focus.
            FocusDependentSelectionColor= Color.GREEN;
            else
            FocusDependentSelectionColor= table.getSelectionBackground();
        if (isSelected)  // override color if selected.
            RenderComponent.setBackground(FocusDependentSelectionColor);
            else
            RenderComponent.setBackground(table.getBackground());
        // RenderComponent.setBackground(Color.RED);  // ???

        return RenderComponent;
        } // getTableCellRendererComponent(.)
    } // class DirectoryTableCellRenderer

