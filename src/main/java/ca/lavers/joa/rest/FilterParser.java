package ca.lavers.joa.rest;

import ca.lavers.jstatemachine.*;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static ca.lavers.jstatemachine.Actions.*;

public class FilterParser {

    public static Filtering parse(String spec) {
        return filterMaker.process(tokenizer.process(TokenStream.of(spec))).next().orElse(null);
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
    private static final StateMachine<Token, Filtering> filterMaker;

    // Matches a digit (0-9)
    private static final Matcher<Character, Token> digit = ctx -> {
        final char c = ctx.currentItem();
        return c >= '0' && c <= '9';
    };

    static {
        tokenizer = new StateMachineBuilder<Character, Token>()
                .state("pre_name")
                    .on(' ')
                    .on('(', error("Missing filter name before opening parenthesis"))
                    .onEnd(error("Expected end of input"))
                    .otherwise(accept(), state("name"))
                .state("name")
                    .on('(', emit("name"), state("pre_arg"))
                    .on(' ', state("pre_parenthesis"))
                    .onEnd(emit("name"))
                    .otherwise(accept())
                .state("pre_parenthesis")
                    .on(' ')
                    .on('(', state("pre_arg"))
                    .onEnd(emit("name"))
                    .otherwise(error("Expected opening parenthesis"))
                .state("pre_arg")
                    .on(' ')
                    .on(')', state("done"))
                    .on('"', state("string"))
                    .on(digit, accept(), state("numeric"))
                    .onEnd(error("Expected end of input"))
                    .otherwise(error("Expected argument"))
                .state("string")
                    .on('"', emit("string"), state("pre_comma"))
                    .on('\\', call("escaped"))
                    .on(',', emit("string"), state("pre_arg"))
                    .on(')', emit("string"), state("done"))
                    .onEnd(error("Expected end of input"))
                    .otherwise(accept())
                .state("escaped")
                    .onEnd(error("Expected escaped character"))
                    .otherwise(accept(), ret())
                .state("numeric")   // TODO -- Only supports positive ints and digit kebabs (i.e. maybe dates)
                    .on(digit, accept())
                    .on('-', accept())
                    .on(',', emit("numeric"), state("pre_arg"))
                    .on(')', emit("numeric"), state("done"))
                    .onEnd(error("Expected end of input"))
                    .otherwise(error("Unexpected character"))
                .state("pre_comma")
                    .on(' ')
                    .on(',', state("pre_arg"))
                    .on(')', state("done"))
                    .onEnd(error("Expected closing parenthesis"))
                    .otherwise(error("Expected comma or closing parenthesis"))
                .state("done")
                    .on(' ')
                    .otherwise(error("Expected end of input"))
                .build();

        filterMaker = new StateMachineBuilder<Token, Filtering>()
                .setMatcherWrapper(m -> ctx -> m.equals(ctx.currentItem().type))
                .state("start")
                    .on("name", acceptName(), state("args"))
                    .onEnd(error("Missing filter name")) // Tokenizer should prevent this
                    .otherwise(error("Missing filter name")) // and this
                .state("args")
                    .on("string", acceptStringArg())
                    .on("numeric", acceptNumericArg())
                    .onEnd(emitFilter())
                    .otherwise(error("Expected argument*"))  // Tokenizer should prevent this
//                    .otherwise(ctx -> {
//                        throw new StateMachineException("WTF: " + ctx.currentItem().type + " " + ctx.currentItem().value, ctx);
//                    })
                .build();

    }

    // Custom action which appends the current character to an internal buffer to
    // be emitted later
    private static Action<Character, Token> accept() {
        return ctx -> {
            StringBuilder buffer = ctx.get("characterBuffer", StringBuilder.class);
            if(buffer == null) {
                buffer = new StringBuilder();
                ctx.put("characterBuffer", buffer);
            }
            buffer.append(ctx.currentItem());
        };
    }

    // Custom action which emits (outputs) the internal buffer as a Token with
    // the given type, and then clears the buffer.
    private static Action<Character, Token> emit(final String type) {
        return ctx -> {
            StringBuilder buffer = ctx.get("characterBuffer", StringBuilder.class);
            if(buffer == null) {
                buffer = new StringBuilder();
                ctx.put("characterBuffer", buffer);
            }
            ctx.emit(new Token(type, buffer.toString()));
            buffer.setLength(0);
        };
    }

    private static Action<Token, Filtering> acceptName() {
        return ctx -> {
            ctx.put("filterName", ctx.currentItem().value);
        };
    }

    private static Action<Token, Filtering> acceptStringArg() {
        return ctx -> {
            String arg = ctx.currentItem().value;
            List<Object> args = ctx.get("args", List.class);
            if(args == null) {
                args = new ArrayList<>();
                ctx.put("args", args);
            }
            args.add(arg);
        };
    }

    private static Action<Token, Filtering> acceptNumericArg() {
        Pattern dateMatcher = Pattern.compile("^\\d{4}-\\d{1,2}-\\d{1,2}$");  // TODO -- iffy
        return ctx -> {
            String arg = ctx.currentItem().value;
            List<Object> args = ctx.get("args", List.class);
            if(args == null) {
                args = new ArrayList<>();
                ctx.put("args", args);
            }
            if(dateMatcher.matcher(arg).matches()) {
                args.add(arg);
            }
            else {
                // Must be a number
                try {
                    args.add(Integer.parseInt(arg));
                } catch (NumberFormatException e) {
                    throw new StateMachineException("Error parsing numeric argument: " + arg, ctx);
                }
            }
        };
    }

    private static Action<Token, Filtering> emitFilter() {
        return ctx -> {
          ctx.emit(new Filtering(ctx.get("filterName", String.class), ctx.get("args", List.class)));
        };
    }

}
