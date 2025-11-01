package org.monke.connector;

import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Timestamp;

public class Schemas {

    public static final String NEXT_PAGE = "next_page";

    // Issue fields.
    public static final String OWNER = "owner";
    public static final String REPOSITORY = "repository";
    public static final String CREATED_AT = "created_at";
    public static final String UPDATED_AT = "updated_at";
    public static final String NUMBER = "number";
    public static final String URL = "url";
    public static final String TITLE = "title";
    public static final String STATE = "state";

    // User fields.
    public static final String USER = "user";
    public static final String USER_URL = "url";
    public static final String USER_ID = "id";
    public static final String USER_LOGIN = "login";

    // PR fields.
    public static final String PR = "pull_request";
    public static final String PR_URL = "url";
    public static final String PR_HTML_URL = "html_url";

    // Schema names
    public static final String KEY_SCHEMA_NAME = "org.monke.github.IssueKey";
    public static final String VALUE_SCHEMA_NAME = "org.monke.github.IssueValue";
    public static final String USER_SCHEMA_NAME = "org.monke.github.UserValue";
    public static final String PR_SCHEMA_NAME = "org.monke.github.PrValue";
    
    public static final Schema KEY_SCHEMA = SchemaBuilder.struct()
        .name(KEY_SCHEMA_NAME)
        .version(1)
        .field(OWNER, Schema.STRING_SCHEMA)
        .field(REPOSITORY, Schema.STRING_SCHEMA)
        .field(NUMBER, Schema.INT32_SCHEMA)
        .build();
    
    public static final Schema USER_SCHEMA = SchemaBuilder.struct().name(USER_SCHEMA_NAME)
        .version(1)
        .field(USER_URL, Schema.STRING_SCHEMA)
        .field(USER_ID, Schema.INT32_SCHEMA)
        .field(USER_LOGIN, Schema.STRING_SCHEMA)
        .build();
    
    public static final Schema PR_SCHEMA = SchemaBuilder.struct().name(PR_SCHEMA_NAME)
        .version(1)
        .field(PR_URL, Schema.STRING_SCHEMA)
        .field(PR_HTML_URL, Schema.STRING_SCHEMA)
        .optional()
        .build();

    public static final Schema VALUE_SCHEMA = SchemaBuilder.struct().name(VALUE_SCHEMA_NAME)
        .version(1)
        .field(URL, Schema.STRING_SCHEMA)
        .field(TITLE, Schema.STRING_SCHEMA)
        .field(CREATED_AT, Timestamp.SCHEMA)
        .field(UPDATED_AT, Timestamp.SCHEMA)
        .field(NUMBER, Schema.INT32_SCHEMA)
        .field(STATE, Schema.STRING_SCHEMA)
        .field(USER, USER_SCHEMA)
        .field(PR, PR_SCHEMA)
        .build();
}
