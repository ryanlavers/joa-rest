package ca.lavers.joa.rest;

import ca.lavers.jstatemachine.*;

import java.util.ArrayList;
import java.util.List;

import static ca.lavers.jstatemachine.Actions.error;
import static ca.lavers.jstatemachine.Actions.state;

class SortParser {

    // TODO - Better error messages
    // TODO - Sanity checks (not repeating fields, etc)

    public static Sorting parse(String spec) {
        List<SortField> fields = new ArrayList<>();
        fieldMaker.process(tokenizer.process(TokenStream.of(spec))).consume(fields::add);
        return new Sorting(fields);
    }

    private static class Token {
        public String type;
        public String value;

        public Token(String type, String value) {
            this.type = type;
            this.value = value;
        }
    }

    private static final StateMachine<Character, Token> tokenizer;
    private static final StateMachine<Token, SortField> fieldMaker;

    static {
        // TODO - regex matchers (or char classes) to limit valid field names
        tokenizer = new StateMachineBuilder<Character, Token>()
                .setContextInitializer(ctx -> {
                    // Our custom actions expect this attribute to exist
                    ctx.put("characterBuffer", new StringBuilder());
                })
                .state("pre_field")
                    .on(' ')
                    .on(',', error("Missing field name before comma"))
                    .otherwise(accept(), state("field"))
                .state("field")
                    .on(' ', emit("field"), state("pre_direction"))
                    .on(',', emit("field"), state("pre_field"))
                    .otherwise(accept())
                    .onEnd(emit("field"))
                .state("pre_direction")
                    .on(' ')
                    .on(',', state("pre_field"))
                    .otherwise(accept(), state("direction"))
                .state("direction")
                    .on(' ', emit("direction"), state("pre_comma"))
                    .on(',', emit("direction"), state("pre_field"))
                    .onEnd(emit("direction"))
                    .otherwise(accept())
                .state("pre_comma")
                    .on(',', state("pre_field"))
                    .on(' ')
                    .otherwise(error("Expected comma"))
                .build();

        fieldMaker = new StateMachineBuilder<Token, SortField>()
                .setMatcherWrapper(m -> ctx -> m.equals(ctx.currentItem().type))
                .state("field")
                    .on("field", acceptField(), state("direction"))
                    .on("direction", error("Expected field name, but got sort direction"))  // tokenizer prevents this anyway
                .state("direction")
                    .on("direction", acceptDirection(), emitField(), state("field"))
                    .on("field", emitField(), acceptField())
                    .onEnd(emitField())
                .build();

    }

    // Custom action which appends the current character to an internal buffer to
    // be emitted later
    private static Action<Character, Token> accept() {
        return ctx -> {
            ctx.get("characterBuffer", StringBuilder.class).append(ctx.currentItem());
        };
    }

    // Custom action which emits (outputs) the internal buffer as a Token with
    // the given type, and then clears the buffer.
    private static Action<Character, Token> emit(final String type) {
        return ctx -> {
            StringBuilder builder = ctx.get("characterBuffer", StringBuilder.class);
            ctx.emit(new Token(type, builder.toString()));
            builder.setLength(0);
        };
    }

    private static Action<Token, SortField> acceptField() {
        return ctx -> {
            ctx.put("field", ctx.currentItem().value);
        };
    }

    private static Action<Token, SortField> acceptDirection() {
        return ctx -> {
            SortDirection direction = switch(ctx.currentItem().value) {
                case "asc", "ASC" -> SortDirection.ASCENDING;
                case "desc", "DESC" -> SortDirection.DESCENDING;
                default -> {
                    throw new StateMachineException("Unrecognized sort direction", ctx);
                }
            };

            ctx.put("direction", direction);
        };
    }

    private static Action<Token, SortField> emitField() {
        return ctx -> {
            SortDirection direction = ctx.get("direction", SortDirection.class);
            if(direction == null) {
                direction = SortDirection.ASCENDING;
            }
            ctx.emit(new SortField(
                    ctx.get("field", String.class),
                    direction
            ));
            ctx.remove("field");
            ctx.remove("direction");
        };
    }

}
