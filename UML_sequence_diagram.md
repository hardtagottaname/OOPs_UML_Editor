# OOPS UML Editor - Sequence Diagrams

This document summarizes the main runtime interactions in the Java Swing UML editor.

## 1. Application Startup

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant MainThread as main()
    participant EDT as Swing EDT
    participant Main as Main JFrame
    participant Canvas as CanvasPanel
    participant Menu as JMenuBar

    User->>MainThread: Launch application
    MainThread->>EDT: SwingUtilities.invokeLater(...)
    EDT->>Main: new Main()
    Main->>Main: configure JFrame
    Main->>Main: initializeComponents()
    Main->>Canvas: new CanvasPanel()
    Canvas->>Canvas: register MouseHandler
    Main->>Main: create tool buttons
    Main->>Canvas: setCurrentMode("select")
    Main->>Menu: setupMenuBar()
    Main->>Main: add buttonPanel and canvasPanel
    EDT->>Main: setVisible(true)
```

## 2. Create Class or Use Case Object

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant Button as rect / oval JButton
    participant Handler as Main.ToolButtonHandler
    participant Main as Main
    participant Canvas as CanvasPanel
    participant Object as UMLObject

    User->>Button: Press rect or oval tool
    Button->>Handler: mousePressed(event)
    Handler->>Main: clearButtonStates()
    Handler->>Main: setButtonActive(button, true)
    Handler->>Canvas: setCurrentMode("rect" or "oval")
    Canvas->>Canvas: clearTemporaryState()
    Canvas->>Canvas: repaint()

    User->>Button: Release over canvas
    Button->>Handler: mouseReleased(event)
    Handler->>Handler: convert button point to canvas point
    alt Release point is inside CanvasPanel
        Handler->>Canvas: createShapeAt(mode, canvasPoint)
        alt mode == "oval"
            Canvas->>Object: new UseCaseObject(x, y)
        else mode == "rect"
            Canvas->>Object: new ClassObject(x, y)
        end
        Canvas->>Object: setDepth(nextBackDepth())
        Canvas->>Canvas: objects.add(object)
        Canvas->>Canvas: repaint()
    end
    Handler->>Main: restore previous active tool
    Handler->>Canvas: setCurrentMode(previous mode)
```

## 3. Create Association / Generalization / Composition Line

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant ToolButton as Line Tool Button
    participant ToolHandler as Main.ToolButtonHandler
    participant Canvas as CanvasPanel
    participant Mouse as CanvasPanel.MouseHandler
    participant StartPort as Port
    participant EndPort as Port
    participant Line as Line

    User->>ToolButton: Click association / generalization / composition
    ToolButton->>ToolHandler: mousePressed(event)
    ToolHandler->>Canvas: setCurrentMode(line mode)
    Canvas->>Canvas: clearTemporaryState()
    Canvas->>Canvas: repaint()

    User->>Canvas: Press on source object's port
    Canvas->>Mouse: mousePressed(event)
    Mouse->>Mouse: isLineMode()
    Mouse->>Canvas: findPortAt(point)
    Canvas->>StartPort: contains(point.x, point.y)
    StartPort->>StartPort: updatePosition()
    Canvas-->>Mouse: linkStartPort
    Mouse->>Mouse: linkPreviewEnd = point

    User->>Canvas: Drag toward target port
    Canvas->>Mouse: mouseDragged(event)
    Mouse->>Mouse: linkPreviewEnd = current point
    Mouse->>Canvas: repaint()
    Canvas->>Canvas: paintComponent(...)
    Canvas->>Canvas: draw dashed preview line

    User->>Canvas: Release on target port
    Canvas->>Mouse: mouseReleased(event)
    Mouse->>Canvas: findPortAt(point)
    Canvas-->>Mouse: endPort
    alt Valid target port on different object
        Mouse->>Line: new Line(linkStartPort, endPort, currentMode)
        Mouse->>Canvas: lines.add(line)
    end
    Mouse->>Mouse: clear linkStartPort and preview
    Mouse->>Canvas: repaint()
    Canvas->>Canvas: drawLine(...)
```

## 4. Select, Move, and Resize Object

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant Canvas as CanvasPanel
    participant Mouse as CanvasPanel.MouseHandler
    participant Object as UMLObject
    participant Port as Port
    participant AbstractObj as AbstractUMLObject

    User->>Canvas: Press in select mode
    Canvas->>Mouse: mousePressed(event)
    Mouse->>Canvas: findPortAt(point)
    alt Pressed on a port
        Canvas-->>Mouse: resizePort
        Mouse->>Port: getParentShape()
        Mouse->>Canvas: getOppositeAnchor(object, port)
        Mouse->>Object: getBounds()
        Mouse->>Canvas: selectSingle(object)
        Canvas->>Canvas: bringToFront(object)
        Mouse->>Canvas: repaint()

        User->>Canvas: Drag resize handle
        Canvas->>Mouse: mouseDragged(event)
        Mouse->>AbstractObj: resizeFromBounds(startBounds, port, anchor, point, MIN_SIZE)
        AbstractObj->>AbstractObj: calculate new bounds
        Mouse->>Canvas: repaint()
    else Pressed on object body
        Canvas->>Canvas: findObjectAt(point)
        Canvas-->>Mouse: hit object
        Mouse->>Canvas: selectSingle(hit)
        Canvas->>Canvas: bringToFront(hit)
        Mouse->>Mouse: movingSelection = true
        Mouse->>Canvas: repaint()

        User->>Canvas: Drag selected object
        Canvas->>Mouse: mouseDragged(event)
        Mouse->>Object: move(dx, dy)
        Mouse->>Mouse: lastMousePoint = point
        Mouse->>Canvas: repaint()
    else Pressed empty canvas
        Mouse->>Canvas: selectedObjects.clear()
        Mouse->>Mouse: start rubber-band selection
        Mouse->>Canvas: repaint()
    end

    User->>Canvas: Release mouse
    Canvas->>Mouse: mouseReleased(event)
    Mouse->>Mouse: clear resize / move / rubber-band state
    Mouse->>Canvas: repaint()
```

## 5. Group and Ungroup

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant Menu as Edit Menu
    participant Canvas as CanvasPanel
    participant Group as CompositeShape
    participant Child as UMLObject

    User->>Menu: Click Group
    Menu->>Canvas: groupSelected()
    alt select mode and at least two selected objects
        Canvas->>Canvas: collect selected children in drawing order
        Canvas->>Canvas: objects.removeAll(children)
        Canvas->>Group: new CompositeShape(children)
        Canvas->>Canvas: objects.add(group)
        Canvas->>Canvas: selectedObjects.clear()
        Canvas->>Canvas: selectedObjects.add(group)
        Canvas->>Canvas: bringToFront(group)
        Canvas->>Canvas: repaint()
    end

    User->>Menu: Click Ungroup
    Menu->>Canvas: ungroupSelected()
    alt select mode and one CompositeShape selected
        Canvas->>Canvas: objects.remove(group)
        Group->>Canvas: getChildren()
        Canvas->>Child: restore child objects
        Canvas->>Canvas: selectedObjects.addAll(children)
        Canvas->>Canvas: normalizeDepths()
        Canvas->>Canvas: repaint()
    end
```

## 6. Change Object Label

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant Menu as Edit Menu
    participant Main as Main
    participant Canvas as CanvasPanel
    participant Dialog as JOptionPane / ColorChooser
    participant Object as UMLObject

    User->>Menu: Click Label
    Menu->>Main: showLabelDialog()
    Main->>Canvas: getSelectedShapes()
    alt Exactly one selected basic object
        Main->>Object: getName()
        Main->>Object: getLabelColor()
        Main->>Dialog: show label form
        opt User chooses color
            Dialog->>Dialog: JColorChooser.showDialog(...)
        end
        User->>Dialog: Click OK
        Dialog-->>Main: OK_OPTION
        Main->>Object: setName(nameField.getText())
        Main->>Object: setLabelColor(selectedColor)
        Main->>Canvas: repaint()
    else Invalid selection
        Main->>Dialog: showMessageDialog(...)
    end
```
