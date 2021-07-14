# PonjiECS
## Overview
Ponji is designed under an ECS pattern. There are no explicit game object classes.


### Entity
Each game object is an **(E)ntity**, which is simply an integer ID and an integer bitmask representing associated components.

### Component
**(C)omponents** are pieces of data (such as arrays, vectors, floats, ints, booleans, etc.) that can be attached to entities. Components are typically used to store data,
but can also be used as flags or to store state.

### System
**(S)ystems** perform logic on all entities that contain data relevant to the system (i.e., position and velocity components in a Movemement system). 
Entities nor components perform logic on entities, all of this is done within systems. Systems are modular, and serve very specific purposes.

## Implementation and Usage
### Initialization
```java
// the creation of the component and system classes is discussed later
private Component1 component1;
private Component2 component2;
private Component3 component3;

private System1 sys1;
private System2 sys2;
private RenderSys renderSys;

private ECS ecs;

public Application() {
  // initialize the ecs with our desired WIDTH and HEIGHT
  ecs = new ECS(WIDTH, HEIGHT);
  
  // this implementation uses a dedicated render system to interface with an extra-ECS renderer
  ecs.addRenderer(
    renderSys = new RenderSys(ecs.width, ecs.height)
  );
  ecs.setWindow(renderSys.getWindow());
  
  ...
}
```

### Adding components and systems to the ECS
```java
...

public Application() {
  // initialize the ecs with our desired WIDTH and HEIGHT
  ecs = new ECS(WIDTH, HEIGHT);
  
  // add components to the ECS
  ecs.addComponent(
    component1 = new Component1(),
    component2 = new Component2(),
    component3 = new Component3()
  );
  
  // add systems to the ECS
  ecs.addSystem(
     sys1 = new System1(),
     sys2 = new System2()
  );
  
  ...
}
```

### Component Implementation
To understand how components will be added/removed and get/set, we must understand what a component class looks like. Each component (Velocity, Position, Rotation, etc.) will
have their own classes, which extend the `Component` class. An example component class for a Radius component is shown below.
```java
import ecs.Component;
import util.Container;

public class Radius extends Component {
    private Container radius;

    public Radius(){
        radius = new Container();
    }

    public int getRadius(int index){
        return (int)radius.get(index);
    }

    public void setRadius(int index, int radius){
        radius.set(index, radius);
    }

    public Radius add(int radius){
        super.setLastWriteIndex(this.radius.add(radius));
        return this;
    }
}
```
Every component class must contain an `add` method, constructed similarly to the above (the only difference is the type of data being added). The get and set methods can be
constructed as desired. The `add` method communicates with the ECS to ensure that the component data is stored properly.

### System Implementation
The context for most logic will be inside of a class that extends the `System` class. Most entity/component interactions will take place inside of system classes. At the bare
minimum, a system class must be constructed as follows:
```java
import ecs.Entity;
import ecs.System;
import ecs.Components.*;
import util.*;
import util.ETree.EntNode;

public class ExampleSys extends System {
    public ExampleSys() {
        super();
    }
    
    // this method is called from within the ECS every frame
    // this is where the system does its work
    @Override
    public Class update(float dt, EntNode entityTree, ComponentMask components) {
            return null;
        }
    }
    
    @Override
    public void exit() {

    }
}
```
`dt` serves as a time step (with respect to the time per frame), `entityTree` stores all entities within the ECS, and `components` stores all components added to the ECS.

### Creating and destroying entities
By this point, our components and systems are already initialized and have been added to the ECS. We supply entities with their components at their creation. This will almost
always occur within a system. To extend the last example, the next example will take place within a system.
```java
public class ExampleSys extends System {
    private Component1 component1;
    private Component2 component2;
    private Component3 component3;

    public ExampleSys() {
        super();
    }
    
    @Override
    public Class update(float dt, EntNode entityTree, ComponentMask components) {
        // retrieve component class instances (stored in the ECS)
        component1 = components.getComponent(Component1.class)
        component2 = components.getComponent(Component2.class)
        component3 = components.getComponent(Component3.class)
    
        // create an entity and (optionally) store its instance in 'entity'
        Entity entity = ecs.createEntity(
            component1.add(14),
            component2.add(new Vector3f(0, 1, 1),
            component3.add(false)
        );
        
        // destroy an entity
        ecs.destroyEntity(entity)
        
        return null;
    }
    
    @Override
    public void exit() {

    }
}
```

### Adding and removing components from a pre-existing entity
Assuming our component classes have been retrieved from the ECS (as shown in the above example), we can add or remove components from entities. I have found that I rarely
use this, but it is provided nonetheless.
```java
// add a new component(s)
ecs.addEntityComponent(entity, exComponent1, exCpomponent2);

// remove an existing component(s)
ecs.removeEntityComponent(entity, exComponent1);
```

### Retrieving entities that contain desired components
```java
public class ExampleSys extends System {
    private Component1 component1;
    private Component2 component2;
    private Component3 component3;

    public ExampleSys() {
        super();
    }
    
    @Override
    public Class update(float dt, EntNode entityTree, ComponentMask components) {
        // retrieve entities that contain Component1 and Component3 (position and velocity, for example)
        Entity[] entities = getEntities(entityTree, new Class[]{Component1.class, Component3.class});
        
        return null;
    }
    
    @Override
    public void exit() {

    }
}
```

### Retrieving/setting entity component values
Let's assume that we want to get the position of an entity (which has a position component)
```java
public class ExampleSys extends System {
    private Position position; 
    
    public ExampleSys() {
        super();
    }
    
    @Override
    public Class update(float dt, EntNode entityTree, ComponentMask components) {
        // retrieve component class instance (stored in the ECS)
        position = components.getComponent(Position.class)
        
        // retrieve entities that contain Component2
        Entity[] entities = getEntities(entityTree, new Class[]{Position.class});
        
        // arbitrarily pick the first entity
        Entity entity = entities[0];
        
        // get position
        Vector3f entityPos = position.getPosition(entity)
        
        // set position
        position.setPosition(new Vector3f(0,0,5), entity)
        
        return null;
    }
}
```
