Java library for object oriented exception handling.
Library converts try/catch/finally statements into reusable objects.

## Usage
### Try/Catch/Finally
Lets assume following simplified scenario (Fetching entity from DB).
```java
public Entity getEntity(String id) throws MyAppException {
    try {
        return entities.get(id);
    } catch(ClientException exp) {
        LOGGER.error("Client exception message", e);
        throw new MyAppException(e);
    } catch(DatabaseException exp) {
        LOGGER.error("Database exception message", e);
        throw new MyAppException(e);
    } catch(ValidationException | IllegalStateException exp) {
        LOGGER.error("Validation exception message", e);
        throw new MyAppException(e);
    } finally {
        LOGGER.info("Attempt to fetch entity");
    }
}
```
Checked exceptions: MyAppException, ClientException and DatabaseException

Runtime exceptions: ValidationException, IllegalStateException

The above code is very procedural and "go-to" like.
We can use objects instead:
```java
public Entity getEntity(String id) throws MyAppException {
    return
        new Try(
            new Catch(
                ClientException.class,
                exp -> LOGGER.error("Client exception message", e)
            ),
            new Catch(
                DatabaseException.class,
                exp -> LOGGER.error("Database exception meassage", e)
            ),
            new Catch(
                new Array<>(ValidationException.class, IllegalStateException.class),
                exp -> LOGGER.error("Validation exception meassage", e)
            )
        ).with(
            new Finally(() -> LOGGER.info("Attempt to fetch entity")),
            new Throws(MyAppException::new)
        ).exec(() -> entities.get(id));
```
Declared Throws instance maps any checked exception to MyAppException and specified
runtime exceptions (ValidationException and IllegalStateException) to MyAppException. Exceptions like NullPointerException etc... are not mapped.

### Try/Finally
```java
new Try().with(new Finally(() -> LOGGER.info("message")))
         .exec(() -> doSomething());
```
### Unchecked exception handling
There are two ways to avoid throwing checked exceptions:
```java
public Entity getEntity(String id) {
    return
        new Try(
            ...
        ).with(new Throws(MyAppRuntimeException::new)
        ).exec(() -> entities.get(id));
}
```
or
```java
public Entity getEntity(String id) {
    return
        new UncheckedTry(
            new Try(
                ...
            )
        ).exec(() -> entities.get(id));
}
```
## Benefits?
    * Declarative instead of procedural code. Easier to maintain, extend and test
    * Classes can be decoupled from exception handling and specific implementation
      can be injected into them
    * SRP compliant
    * Reusable
### Exception handling injection
```java
public class MyApp {

    private final Checkable<MyAppException> checkable;

    public MyApp(Checkable<MyAppException> checkable) {
        this.checkable = checkable;
    }

    public Entity getEntity(String id) throws MyAppException{
        return this.checkable.exec(() -> entities.get(id));
    }
}
```

