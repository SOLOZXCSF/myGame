package com.example.game.ecs;

import com.example.game.ecs.component.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Базовая игровая сущность: просто ID + живость + набор компонентов.
 *
 * Компоненты получаются по типу:
 *   entity.get(TransformComponent.class).ifPresent(t -> ...);
 *
 * Для конкретных сущностей (Player, Zombie, …) создаются подклассы,
 * которые в конструкторе кладут нужные компоненты через add().
 */
public abstract class Entity {

    private static int ID_SEQ = 0;

    private final int id;
    protected boolean alive = true;

    private final Map<Class<? extends Component>, Component> components = new HashMap<>();

    protected Entity() {
        this.id = ID_SEQ++;
    }

    protected Entity(int fixedId) {
        this.id = fixedId;
    }

    // ── компоненты ───────────────────────────────────────────────────────

    protected <C extends Component> void add(C component) {
        components.put(component.getClass(), component);
    }

    @SuppressWarnings("unchecked")
    public <C extends Component> Optional<C> get(Class<C> type) {
        return Optional.ofNullable((C) components.get(type));
    }

    /** Быстрый доступ без Optional — бросает исключение если компонента нет. */
    @SuppressWarnings("unchecked")
    public <C extends Component> C require(Class<C> type) {
        C c = (C) components.get(type);
        if (c == null) throw new IllegalStateException(
                getClass().getSimpleName() + " missing component: " + type.getSimpleName());
        return c;
    }

    public boolean has(Class<? extends Component> type) {
        return components.containsKey(type);
    }

    // ── жизнь ────────────────────────────────────────────────────────────

    public boolean isAlive() { return alive; }
    public void    kill()    { alive = false; }

    public int getId() { return id; }
}
