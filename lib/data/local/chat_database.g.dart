// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'chat_database.dart';

// ignore_for_file: type=lint
class $MessagesTable extends Messages with TableInfo<$MessagesTable, Message> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $MessagesTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _messageIdMeta = const VerificationMeta(
    'messageId',
  );
  @override
  late final GeneratedColumn<String> messageId = GeneratedColumn<String>(
    'message_id',
    aliasedName,
    false,
    type: DriftSqlType.string,
    requiredDuringInsert: true,
  );
  static const VerificationMeta _messageOrderMeta = const VerificationMeta(
    'messageOrder',
  );
  @override
  late final GeneratedColumn<int> messageOrder = GeneratedColumn<int>(
    'message_order',
    aliasedName,
    false,
    type: DriftSqlType.int,
    requiredDuringInsert: true,
  );
  static const VerificationMeta _accountIdMeta = const VerificationMeta(
    'accountId',
  );
  @override
  late final GeneratedColumn<int> accountId = GeneratedColumn<int>(
    'account_id',
    aliasedName,
    false,
    type: DriftSqlType.int,
    requiredDuringInsert: true,
  );
  static const VerificationMeta _conversationIdMeta = const VerificationMeta(
    'conversationId',
  );
  @override
  late final GeneratedColumn<String> conversationId = GeneratedColumn<String>(
    'conversation_id',
    aliasedName,
    false,
    type: DriftSqlType.string,
    requiredDuringInsert: true,
  );
  static const VerificationMeta _turnIdMeta = const VerificationMeta('turnId');
  @override
  late final GeneratedColumn<String> turnId = GeneratedColumn<String>(
    'turn_id',
    aliasedName,
    false,
    type: DriftSqlType.string,
    requiredDuringInsert: true,
  );
  static const VerificationMeta _roleMeta = const VerificationMeta('role');
  @override
  late final GeneratedColumn<String> role = GeneratedColumn<String>(
    'role',
    aliasedName,
    false,
    type: DriftSqlType.string,
    requiredDuringInsert: true,
  );
  static const VerificationMeta _contentMeta = const VerificationMeta(
    'content',
  );
  @override
  late final GeneratedColumn<String> content = GeneratedColumn<String>(
    'content',
    aliasedName,
    false,
    type: DriftSqlType.string,
    requiredDuringInsert: true,
  );
  static const VerificationMeta _attachmentsMeta = const VerificationMeta(
    'attachments',
  );
  @override
  late final GeneratedColumn<String> attachments = GeneratedColumn<String>(
    'attachments',
    aliasedName,
    false,
    type: DriftSqlType.string,
    requiredDuringInsert: false,
    defaultValue: const Constant('[]'),
  );
  static const VerificationMeta _replyToIdMeta = const VerificationMeta(
    'replyToId',
  );
  @override
  late final GeneratedColumn<String> replyToId = GeneratedColumn<String>(
    'reply_to_id',
    aliasedName,
    true,
    type: DriftSqlType.string,
    requiredDuringInsert: false,
  );
  static const VerificationMeta _replyToContentMeta = const VerificationMeta(
    'replyToContent',
  );
  @override
  late final GeneratedColumn<String> replyToContent = GeneratedColumn<String>(
    'reply_to_content',
    aliasedName,
    true,
    type: DriftSqlType.string,
    requiredDuringInsert: false,
  );
  static const VerificationMeta _replyToRoleMeta = const VerificationMeta(
    'replyToRole',
  );
  @override
  late final GeneratedColumn<String> replyToRole = GeneratedColumn<String>(
    'reply_to_role',
    aliasedName,
    true,
    type: DriftSqlType.string,
    requiredDuringInsert: false,
  );
  static const VerificationMeta _createdAtMeta = const VerificationMeta(
    'createdAt',
  );
  @override
  late final GeneratedColumn<DateTime> createdAt = GeneratedColumn<DateTime>(
    'created_at',
    aliasedName,
    false,
    type: DriftSqlType.dateTime,
    requiredDuringInsert: true,
  );
  @override
  List<GeneratedColumn> get $columns => [
    messageId,
    messageOrder,
    accountId,
    conversationId,
    turnId,
    role,
    content,
    attachments,
    replyToId,
    replyToContent,
    replyToRole,
    createdAt,
  ];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'messages';
  @override
  VerificationContext validateIntegrity(
    Insertable<Message> instance, {
    bool isInserting = false,
  }) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('message_id')) {
      context.handle(
        _messageIdMeta,
        messageId.isAcceptableOrUnknown(data['message_id']!, _messageIdMeta),
      );
    } else if (isInserting) {
      context.missing(_messageIdMeta);
    }
    if (data.containsKey('message_order')) {
      context.handle(
        _messageOrderMeta,
        messageOrder.isAcceptableOrUnknown(
          data['message_order']!,
          _messageOrderMeta,
        ),
      );
    } else if (isInserting) {
      context.missing(_messageOrderMeta);
    }
    if (data.containsKey('account_id')) {
      context.handle(
        _accountIdMeta,
        accountId.isAcceptableOrUnknown(data['account_id']!, _accountIdMeta),
      );
    } else if (isInserting) {
      context.missing(_accountIdMeta);
    }
    if (data.containsKey('conversation_id')) {
      context.handle(
        _conversationIdMeta,
        conversationId.isAcceptableOrUnknown(
          data['conversation_id']!,
          _conversationIdMeta,
        ),
      );
    } else if (isInserting) {
      context.missing(_conversationIdMeta);
    }
    if (data.containsKey('turn_id')) {
      context.handle(
        _turnIdMeta,
        turnId.isAcceptableOrUnknown(data['turn_id']!, _turnIdMeta),
      );
    } else if (isInserting) {
      context.missing(_turnIdMeta);
    }
    if (data.containsKey('role')) {
      context.handle(
        _roleMeta,
        role.isAcceptableOrUnknown(data['role']!, _roleMeta),
      );
    } else if (isInserting) {
      context.missing(_roleMeta);
    }
    if (data.containsKey('content')) {
      context.handle(
        _contentMeta,
        content.isAcceptableOrUnknown(data['content']!, _contentMeta),
      );
    } else if (isInserting) {
      context.missing(_contentMeta);
    }
    if (data.containsKey('attachments')) {
      context.handle(
        _attachmentsMeta,
        attachments.isAcceptableOrUnknown(
          data['attachments']!,
          _attachmentsMeta,
        ),
      );
    }
    if (data.containsKey('reply_to_id')) {
      context.handle(
        _replyToIdMeta,
        replyToId.isAcceptableOrUnknown(data['reply_to_id']!, _replyToIdMeta),
      );
    }
    if (data.containsKey('reply_to_content')) {
      context.handle(
        _replyToContentMeta,
        replyToContent.isAcceptableOrUnknown(
          data['reply_to_content']!,
          _replyToContentMeta,
        ),
      );
    }
    if (data.containsKey('reply_to_role')) {
      context.handle(
        _replyToRoleMeta,
        replyToRole.isAcceptableOrUnknown(
          data['reply_to_role']!,
          _replyToRoleMeta,
        ),
      );
    }
    if (data.containsKey('created_at')) {
      context.handle(
        _createdAtMeta,
        createdAt.isAcceptableOrUnknown(data['created_at']!, _createdAtMeta),
      );
    } else if (isInserting) {
      context.missing(_createdAtMeta);
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {messageId};
  @override
  Message map(Map<String, dynamic> data, {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return Message(
      messageId: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}message_id'],
      )!,
      messageOrder: attachedDatabase.typeMapping.read(
        DriftSqlType.int,
        data['${effectivePrefix}message_order'],
      )!,
      accountId: attachedDatabase.typeMapping.read(
        DriftSqlType.int,
        data['${effectivePrefix}account_id'],
      )!,
      conversationId: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}conversation_id'],
      )!,
      turnId: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}turn_id'],
      )!,
      role: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}role'],
      )!,
      content: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}content'],
      )!,
      attachments: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}attachments'],
      )!,
      replyToId: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}reply_to_id'],
      ),
      replyToContent: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}reply_to_content'],
      ),
      replyToRole: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}reply_to_role'],
      ),
      createdAt: attachedDatabase.typeMapping.read(
        DriftSqlType.dateTime,
        data['${effectivePrefix}created_at'],
      )!,
    );
  }

  @override
  $MessagesTable createAlias(String alias) {
    return $MessagesTable(attachedDatabase, alias);
  }
}

class Message extends DataClass implements Insertable<Message> {
  final String messageId;
  final int messageOrder;
  final int accountId;
  final String conversationId;
  final String turnId;
  final String role;
  final String content;
  final String attachments;
  final String? replyToId;
  final String? replyToContent;
  final String? replyToRole;
  final DateTime createdAt;
  const Message({
    required this.messageId,
    required this.messageOrder,
    required this.accountId,
    required this.conversationId,
    required this.turnId,
    required this.role,
    required this.content,
    required this.attachments,
    this.replyToId,
    this.replyToContent,
    this.replyToRole,
    required this.createdAt,
  });
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['message_id'] = Variable<String>(messageId);
    map['message_order'] = Variable<int>(messageOrder);
    map['account_id'] = Variable<int>(accountId);
    map['conversation_id'] = Variable<String>(conversationId);
    map['turn_id'] = Variable<String>(turnId);
    map['role'] = Variable<String>(role);
    map['content'] = Variable<String>(content);
    map['attachments'] = Variable<String>(attachments);
    if (!nullToAbsent || replyToId != null) {
      map['reply_to_id'] = Variable<String>(replyToId);
    }
    if (!nullToAbsent || replyToContent != null) {
      map['reply_to_content'] = Variable<String>(replyToContent);
    }
    if (!nullToAbsent || replyToRole != null) {
      map['reply_to_role'] = Variable<String>(replyToRole);
    }
    map['created_at'] = Variable<DateTime>(createdAt);
    return map;
  }

  MessagesCompanion toCompanion(bool nullToAbsent) {
    return MessagesCompanion(
      messageId: Value(messageId),
      messageOrder: Value(messageOrder),
      accountId: Value(accountId),
      conversationId: Value(conversationId),
      turnId: Value(turnId),
      role: Value(role),
      content: Value(content),
      attachments: Value(attachments),
      replyToId: replyToId == null && nullToAbsent
          ? const Value.absent()
          : Value(replyToId),
      replyToContent: replyToContent == null && nullToAbsent
          ? const Value.absent()
          : Value(replyToContent),
      replyToRole: replyToRole == null && nullToAbsent
          ? const Value.absent()
          : Value(replyToRole),
      createdAt: Value(createdAt),
    );
  }

  factory Message.fromJson(
    Map<String, dynamic> json, {
    ValueSerializer? serializer,
  }) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return Message(
      messageId: serializer.fromJson<String>(json['messageId']),
      messageOrder: serializer.fromJson<int>(json['messageOrder']),
      accountId: serializer.fromJson<int>(json['accountId']),
      conversationId: serializer.fromJson<String>(json['conversationId']),
      turnId: serializer.fromJson<String>(json['turnId']),
      role: serializer.fromJson<String>(json['role']),
      content: serializer.fromJson<String>(json['content']),
      attachments: serializer.fromJson<String>(json['attachments']),
      replyToId: serializer.fromJson<String?>(json['replyToId']),
      replyToContent: serializer.fromJson<String?>(json['replyToContent']),
      replyToRole: serializer.fromJson<String?>(json['replyToRole']),
      createdAt: serializer.fromJson<DateTime>(json['createdAt']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'messageId': serializer.toJson<String>(messageId),
      'messageOrder': serializer.toJson<int>(messageOrder),
      'accountId': serializer.toJson<int>(accountId),
      'conversationId': serializer.toJson<String>(conversationId),
      'turnId': serializer.toJson<String>(turnId),
      'role': serializer.toJson<String>(role),
      'content': serializer.toJson<String>(content),
      'attachments': serializer.toJson<String>(attachments),
      'replyToId': serializer.toJson<String?>(replyToId),
      'replyToContent': serializer.toJson<String?>(replyToContent),
      'replyToRole': serializer.toJson<String?>(replyToRole),
      'createdAt': serializer.toJson<DateTime>(createdAt),
    };
  }

  Message copyWith({
    String? messageId,
    int? messageOrder,
    int? accountId,
    String? conversationId,
    String? turnId,
    String? role,
    String? content,
    String? attachments,
    Value<String?> replyToId = const Value.absent(),
    Value<String?> replyToContent = const Value.absent(),
    Value<String?> replyToRole = const Value.absent(),
    DateTime? createdAt,
  }) => Message(
    messageId: messageId ?? this.messageId,
    messageOrder: messageOrder ?? this.messageOrder,
    accountId: accountId ?? this.accountId,
    conversationId: conversationId ?? this.conversationId,
    turnId: turnId ?? this.turnId,
    role: role ?? this.role,
    content: content ?? this.content,
    attachments: attachments ?? this.attachments,
    replyToId: replyToId.present ? replyToId.value : this.replyToId,
    replyToContent: replyToContent.present
        ? replyToContent.value
        : this.replyToContent,
    replyToRole: replyToRole.present ? replyToRole.value : this.replyToRole,
    createdAt: createdAt ?? this.createdAt,
  );
  Message copyWithCompanion(MessagesCompanion data) {
    return Message(
      messageId: data.messageId.present ? data.messageId.value : this.messageId,
      messageOrder: data.messageOrder.present
          ? data.messageOrder.value
          : this.messageOrder,
      accountId: data.accountId.present ? data.accountId.value : this.accountId,
      conversationId: data.conversationId.present
          ? data.conversationId.value
          : this.conversationId,
      turnId: data.turnId.present ? data.turnId.value : this.turnId,
      role: data.role.present ? data.role.value : this.role,
      content: data.content.present ? data.content.value : this.content,
      attachments: data.attachments.present
          ? data.attachments.value
          : this.attachments,
      replyToId: data.replyToId.present ? data.replyToId.value : this.replyToId,
      replyToContent: data.replyToContent.present
          ? data.replyToContent.value
          : this.replyToContent,
      replyToRole: data.replyToRole.present
          ? data.replyToRole.value
          : this.replyToRole,
      createdAt: data.createdAt.present ? data.createdAt.value : this.createdAt,
    );
  }

  @override
  String toString() {
    return (StringBuffer('Message(')
          ..write('messageId: $messageId, ')
          ..write('messageOrder: $messageOrder, ')
          ..write('accountId: $accountId, ')
          ..write('conversationId: $conversationId, ')
          ..write('turnId: $turnId, ')
          ..write('role: $role, ')
          ..write('content: $content, ')
          ..write('attachments: $attachments, ')
          ..write('replyToId: $replyToId, ')
          ..write('replyToContent: $replyToContent, ')
          ..write('replyToRole: $replyToRole, ')
          ..write('createdAt: $createdAt')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => Object.hash(
    messageId,
    messageOrder,
    accountId,
    conversationId,
    turnId,
    role,
    content,
    attachments,
    replyToId,
    replyToContent,
    replyToRole,
    createdAt,
  );
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is Message &&
          other.messageId == this.messageId &&
          other.messageOrder == this.messageOrder &&
          other.accountId == this.accountId &&
          other.conversationId == this.conversationId &&
          other.turnId == this.turnId &&
          other.role == this.role &&
          other.content == this.content &&
          other.attachments == this.attachments &&
          other.replyToId == this.replyToId &&
          other.replyToContent == this.replyToContent &&
          other.replyToRole == this.replyToRole &&
          other.createdAt == this.createdAt);
}

class MessagesCompanion extends UpdateCompanion<Message> {
  final Value<String> messageId;
  final Value<int> messageOrder;
  final Value<int> accountId;
  final Value<String> conversationId;
  final Value<String> turnId;
  final Value<String> role;
  final Value<String> content;
  final Value<String> attachments;
  final Value<String?> replyToId;
  final Value<String?> replyToContent;
  final Value<String?> replyToRole;
  final Value<DateTime> createdAt;
  final Value<int> rowid;
  const MessagesCompanion({
    this.messageId = const Value.absent(),
    this.messageOrder = const Value.absent(),
    this.accountId = const Value.absent(),
    this.conversationId = const Value.absent(),
    this.turnId = const Value.absent(),
    this.role = const Value.absent(),
    this.content = const Value.absent(),
    this.attachments = const Value.absent(),
    this.replyToId = const Value.absent(),
    this.replyToContent = const Value.absent(),
    this.replyToRole = const Value.absent(),
    this.createdAt = const Value.absent(),
    this.rowid = const Value.absent(),
  });
  MessagesCompanion.insert({
    required String messageId,
    required int messageOrder,
    required int accountId,
    required String conversationId,
    required String turnId,
    required String role,
    required String content,
    this.attachments = const Value.absent(),
    this.replyToId = const Value.absent(),
    this.replyToContent = const Value.absent(),
    this.replyToRole = const Value.absent(),
    required DateTime createdAt,
    this.rowid = const Value.absent(),
  }) : messageId = Value(messageId),
       messageOrder = Value(messageOrder),
       accountId = Value(accountId),
       conversationId = Value(conversationId),
       turnId = Value(turnId),
       role = Value(role),
       content = Value(content),
       createdAt = Value(createdAt);
  static Insertable<Message> custom({
    Expression<String>? messageId,
    Expression<int>? messageOrder,
    Expression<int>? accountId,
    Expression<String>? conversationId,
    Expression<String>? turnId,
    Expression<String>? role,
    Expression<String>? content,
    Expression<String>? attachments,
    Expression<String>? replyToId,
    Expression<String>? replyToContent,
    Expression<String>? replyToRole,
    Expression<DateTime>? createdAt,
    Expression<int>? rowid,
  }) {
    return RawValuesInsertable({
      if (messageId != null) 'message_id': messageId,
      if (messageOrder != null) 'message_order': messageOrder,
      if (accountId != null) 'account_id': accountId,
      if (conversationId != null) 'conversation_id': conversationId,
      if (turnId != null) 'turn_id': turnId,
      if (role != null) 'role': role,
      if (content != null) 'content': content,
      if (attachments != null) 'attachments': attachments,
      if (replyToId != null) 'reply_to_id': replyToId,
      if (replyToContent != null) 'reply_to_content': replyToContent,
      if (replyToRole != null) 'reply_to_role': replyToRole,
      if (createdAt != null) 'created_at': createdAt,
      if (rowid != null) 'rowid': rowid,
    });
  }

  MessagesCompanion copyWith({
    Value<String>? messageId,
    Value<int>? messageOrder,
    Value<int>? accountId,
    Value<String>? conversationId,
    Value<String>? turnId,
    Value<String>? role,
    Value<String>? content,
    Value<String>? attachments,
    Value<String?>? replyToId,
    Value<String?>? replyToContent,
    Value<String?>? replyToRole,
    Value<DateTime>? createdAt,
    Value<int>? rowid,
  }) {
    return MessagesCompanion(
      messageId: messageId ?? this.messageId,
      messageOrder: messageOrder ?? this.messageOrder,
      accountId: accountId ?? this.accountId,
      conversationId: conversationId ?? this.conversationId,
      turnId: turnId ?? this.turnId,
      role: role ?? this.role,
      content: content ?? this.content,
      attachments: attachments ?? this.attachments,
      replyToId: replyToId ?? this.replyToId,
      replyToContent: replyToContent ?? this.replyToContent,
      replyToRole: replyToRole ?? this.replyToRole,
      createdAt: createdAt ?? this.createdAt,
      rowid: rowid ?? this.rowid,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (messageId.present) {
      map['message_id'] = Variable<String>(messageId.value);
    }
    if (messageOrder.present) {
      map['message_order'] = Variable<int>(messageOrder.value);
    }
    if (accountId.present) {
      map['account_id'] = Variable<int>(accountId.value);
    }
    if (conversationId.present) {
      map['conversation_id'] = Variable<String>(conversationId.value);
    }
    if (turnId.present) {
      map['turn_id'] = Variable<String>(turnId.value);
    }
    if (role.present) {
      map['role'] = Variable<String>(role.value);
    }
    if (content.present) {
      map['content'] = Variable<String>(content.value);
    }
    if (attachments.present) {
      map['attachments'] = Variable<String>(attachments.value);
    }
    if (replyToId.present) {
      map['reply_to_id'] = Variable<String>(replyToId.value);
    }
    if (replyToContent.present) {
      map['reply_to_content'] = Variable<String>(replyToContent.value);
    }
    if (replyToRole.present) {
      map['reply_to_role'] = Variable<String>(replyToRole.value);
    }
    if (createdAt.present) {
      map['created_at'] = Variable<DateTime>(createdAt.value);
    }
    if (rowid.present) {
      map['rowid'] = Variable<int>(rowid.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('MessagesCompanion(')
          ..write('messageId: $messageId, ')
          ..write('messageOrder: $messageOrder, ')
          ..write('accountId: $accountId, ')
          ..write('conversationId: $conversationId, ')
          ..write('turnId: $turnId, ')
          ..write('role: $role, ')
          ..write('content: $content, ')
          ..write('attachments: $attachments, ')
          ..write('replyToId: $replyToId, ')
          ..write('replyToContent: $replyToContent, ')
          ..write('replyToRole: $replyToRole, ')
          ..write('createdAt: $createdAt, ')
          ..write('rowid: $rowid')
          ..write(')'))
        .toString();
  }
}

class $RequestsTable extends Requests with TableInfo<$RequestsTable, Request> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $RequestsTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _requestIdMeta = const VerificationMeta(
    'requestId',
  );
  @override
  late final GeneratedColumn<String> requestId = GeneratedColumn<String>(
    'request_id',
    aliasedName,
    false,
    type: DriftSqlType.string,
    requiredDuringInsert: true,
  );
  static const VerificationMeta _accountIdMeta = const VerificationMeta(
    'accountId',
  );
  @override
  late final GeneratedColumn<int> accountId = GeneratedColumn<int>(
    'account_id',
    aliasedName,
    false,
    type: DriftSqlType.int,
    requiredDuringInsert: true,
  );
  static const VerificationMeta _contentMeta = const VerificationMeta(
    'content',
  );
  @override
  late final GeneratedColumn<String> content = GeneratedColumn<String>(
    'content',
    aliasedName,
    false,
    type: DriftSqlType.string,
    requiredDuringInsert: true,
  );
  static const VerificationMeta _statusMeta = const VerificationMeta('status');
  @override
  late final GeneratedColumn<String> status = GeneratedColumn<String>(
    'status',
    aliasedName,
    false,
    type: DriftSqlType.string,
    requiredDuringInsert: true,
  );
  static const VerificationMeta _createdAtMeta = const VerificationMeta(
    'createdAt',
  );
  @override
  late final GeneratedColumn<DateTime> createdAt = GeneratedColumn<DateTime>(
    'created_at',
    aliasedName,
    false,
    type: DriftSqlType.dateTime,
    requiredDuringInsert: true,
  );
  @override
  List<GeneratedColumn> get $columns => [
    requestId,
    accountId,
    content,
    status,
    createdAt,
  ];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'requests';
  @override
  VerificationContext validateIntegrity(
    Insertable<Request> instance, {
    bool isInserting = false,
  }) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('request_id')) {
      context.handle(
        _requestIdMeta,
        requestId.isAcceptableOrUnknown(data['request_id']!, _requestIdMeta),
      );
    } else if (isInserting) {
      context.missing(_requestIdMeta);
    }
    if (data.containsKey('account_id')) {
      context.handle(
        _accountIdMeta,
        accountId.isAcceptableOrUnknown(data['account_id']!, _accountIdMeta),
      );
    } else if (isInserting) {
      context.missing(_accountIdMeta);
    }
    if (data.containsKey('content')) {
      context.handle(
        _contentMeta,
        content.isAcceptableOrUnknown(data['content']!, _contentMeta),
      );
    } else if (isInserting) {
      context.missing(_contentMeta);
    }
    if (data.containsKey('status')) {
      context.handle(
        _statusMeta,
        status.isAcceptableOrUnknown(data['status']!, _statusMeta),
      );
    } else if (isInserting) {
      context.missing(_statusMeta);
    }
    if (data.containsKey('created_at')) {
      context.handle(
        _createdAtMeta,
        createdAt.isAcceptableOrUnknown(data['created_at']!, _createdAtMeta),
      );
    } else if (isInserting) {
      context.missing(_createdAtMeta);
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {requestId};
  @override
  Request map(Map<String, dynamic> data, {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return Request(
      requestId: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}request_id'],
      )!,
      accountId: attachedDatabase.typeMapping.read(
        DriftSqlType.int,
        data['${effectivePrefix}account_id'],
      )!,
      content: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}content'],
      )!,
      status: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}status'],
      )!,
      createdAt: attachedDatabase.typeMapping.read(
        DriftSqlType.dateTime,
        data['${effectivePrefix}created_at'],
      )!,
    );
  }

  @override
  $RequestsTable createAlias(String alias) {
    return $RequestsTable(attachedDatabase, alias);
  }
}

class Request extends DataClass implements Insertable<Request> {
  final String requestId;
  final int accountId;
  final String content;
  final String status;
  final DateTime createdAt;
  const Request({
    required this.requestId,
    required this.accountId,
    required this.content,
    required this.status,
    required this.createdAt,
  });
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['request_id'] = Variable<String>(requestId);
    map['account_id'] = Variable<int>(accountId);
    map['content'] = Variable<String>(content);
    map['status'] = Variable<String>(status);
    map['created_at'] = Variable<DateTime>(createdAt);
    return map;
  }

  RequestsCompanion toCompanion(bool nullToAbsent) {
    return RequestsCompanion(
      requestId: Value(requestId),
      accountId: Value(accountId),
      content: Value(content),
      status: Value(status),
      createdAt: Value(createdAt),
    );
  }

  factory Request.fromJson(
    Map<String, dynamic> json, {
    ValueSerializer? serializer,
  }) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return Request(
      requestId: serializer.fromJson<String>(json['requestId']),
      accountId: serializer.fromJson<int>(json['accountId']),
      content: serializer.fromJson<String>(json['content']),
      status: serializer.fromJson<String>(json['status']),
      createdAt: serializer.fromJson<DateTime>(json['createdAt']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'requestId': serializer.toJson<String>(requestId),
      'accountId': serializer.toJson<int>(accountId),
      'content': serializer.toJson<String>(content),
      'status': serializer.toJson<String>(status),
      'createdAt': serializer.toJson<DateTime>(createdAt),
    };
  }

  Request copyWith({
    String? requestId,
    int? accountId,
    String? content,
    String? status,
    DateTime? createdAt,
  }) => Request(
    requestId: requestId ?? this.requestId,
    accountId: accountId ?? this.accountId,
    content: content ?? this.content,
    status: status ?? this.status,
    createdAt: createdAt ?? this.createdAt,
  );
  Request copyWithCompanion(RequestsCompanion data) {
    return Request(
      requestId: data.requestId.present ? data.requestId.value : this.requestId,
      accountId: data.accountId.present ? data.accountId.value : this.accountId,
      content: data.content.present ? data.content.value : this.content,
      status: data.status.present ? data.status.value : this.status,
      createdAt: data.createdAt.present ? data.createdAt.value : this.createdAt,
    );
  }

  @override
  String toString() {
    return (StringBuffer('Request(')
          ..write('requestId: $requestId, ')
          ..write('accountId: $accountId, ')
          ..write('content: $content, ')
          ..write('status: $status, ')
          ..write('createdAt: $createdAt')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode =>
      Object.hash(requestId, accountId, content, status, createdAt);
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is Request &&
          other.requestId == this.requestId &&
          other.accountId == this.accountId &&
          other.content == this.content &&
          other.status == this.status &&
          other.createdAt == this.createdAt);
}

class RequestsCompanion extends UpdateCompanion<Request> {
  final Value<String> requestId;
  final Value<int> accountId;
  final Value<String> content;
  final Value<String> status;
  final Value<DateTime> createdAt;
  final Value<int> rowid;
  const RequestsCompanion({
    this.requestId = const Value.absent(),
    this.accountId = const Value.absent(),
    this.content = const Value.absent(),
    this.status = const Value.absent(),
    this.createdAt = const Value.absent(),
    this.rowid = const Value.absent(),
  });
  RequestsCompanion.insert({
    required String requestId,
    required int accountId,
    required String content,
    required String status,
    required DateTime createdAt,
    this.rowid = const Value.absent(),
  }) : requestId = Value(requestId),
       accountId = Value(accountId),
       content = Value(content),
       status = Value(status),
       createdAt = Value(createdAt);
  static Insertable<Request> custom({
    Expression<String>? requestId,
    Expression<int>? accountId,
    Expression<String>? content,
    Expression<String>? status,
    Expression<DateTime>? createdAt,
    Expression<int>? rowid,
  }) {
    return RawValuesInsertable({
      if (requestId != null) 'request_id': requestId,
      if (accountId != null) 'account_id': accountId,
      if (content != null) 'content': content,
      if (status != null) 'status': status,
      if (createdAt != null) 'created_at': createdAt,
      if (rowid != null) 'rowid': rowid,
    });
  }

  RequestsCompanion copyWith({
    Value<String>? requestId,
    Value<int>? accountId,
    Value<String>? content,
    Value<String>? status,
    Value<DateTime>? createdAt,
    Value<int>? rowid,
  }) {
    return RequestsCompanion(
      requestId: requestId ?? this.requestId,
      accountId: accountId ?? this.accountId,
      content: content ?? this.content,
      status: status ?? this.status,
      createdAt: createdAt ?? this.createdAt,
      rowid: rowid ?? this.rowid,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (requestId.present) {
      map['request_id'] = Variable<String>(requestId.value);
    }
    if (accountId.present) {
      map['account_id'] = Variable<int>(accountId.value);
    }
    if (content.present) {
      map['content'] = Variable<String>(content.value);
    }
    if (status.present) {
      map['status'] = Variable<String>(status.value);
    }
    if (createdAt.present) {
      map['created_at'] = Variable<DateTime>(createdAt.value);
    }
    if (rowid.present) {
      map['rowid'] = Variable<int>(rowid.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('RequestsCompanion(')
          ..write('requestId: $requestId, ')
          ..write('accountId: $accountId, ')
          ..write('content: $content, ')
          ..write('status: $status, ')
          ..write('createdAt: $createdAt, ')
          ..write('rowid: $rowid')
          ..write(')'))
        .toString();
  }
}

class $FileTransfersTable extends FileTransfers
    with TableInfo<$FileTransfersTable, FileTransfer> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $FileTransfersTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _idMeta = const VerificationMeta('id');
  @override
  late final GeneratedColumn<String> id = GeneratedColumn<String>(
    'id',
    aliasedName,
    false,
    type: DriftSqlType.string,
    requiredDuringInsert: true,
  );
  static const VerificationMeta _uploadedBytesMeta = const VerificationMeta(
    'uploadedBytes',
  );
  @override
  late final GeneratedColumn<int> uploadedBytes = GeneratedColumn<int>(
    'uploaded_bytes',
    aliasedName,
    false,
    type: DriftSqlType.int,
    requiredDuringInsert: false,
    defaultValue: const Constant(0),
  );
  static const VerificationMeta _downloadedBytesMeta = const VerificationMeta(
    'downloadedBytes',
  );
  @override
  late final GeneratedColumn<int> downloadedBytes = GeneratedColumn<int>(
    'downloaded_bytes',
    aliasedName,
    false,
    type: DriftSqlType.int,
    requiredDuringInsert: false,
    defaultValue: const Constant(0),
  );
  static const VerificationMeta _statusMeta = const VerificationMeta('status');
  @override
  late final GeneratedColumn<String> status = GeneratedColumn<String>(
    'status',
    aliasedName,
    false,
    type: DriftSqlType.string,
    requiredDuringInsert: true,
  );
  @override
  List<GeneratedColumn> get $columns => [
    id,
    uploadedBytes,
    downloadedBytes,
    status,
  ];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'file_transfers';
  @override
  VerificationContext validateIntegrity(
    Insertable<FileTransfer> instance, {
    bool isInserting = false,
  }) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('id')) {
      context.handle(_idMeta, id.isAcceptableOrUnknown(data['id']!, _idMeta));
    } else if (isInserting) {
      context.missing(_idMeta);
    }
    if (data.containsKey('uploaded_bytes')) {
      context.handle(
        _uploadedBytesMeta,
        uploadedBytes.isAcceptableOrUnknown(
          data['uploaded_bytes']!,
          _uploadedBytesMeta,
        ),
      );
    }
    if (data.containsKey('downloaded_bytes')) {
      context.handle(
        _downloadedBytesMeta,
        downloadedBytes.isAcceptableOrUnknown(
          data['downloaded_bytes']!,
          _downloadedBytesMeta,
        ),
      );
    }
    if (data.containsKey('status')) {
      context.handle(
        _statusMeta,
        status.isAcceptableOrUnknown(data['status']!, _statusMeta),
      );
    } else if (isInserting) {
      context.missing(_statusMeta);
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {id};
  @override
  FileTransfer map(Map<String, dynamic> data, {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return FileTransfer(
      id: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}id'],
      )!,
      uploadedBytes: attachedDatabase.typeMapping.read(
        DriftSqlType.int,
        data['${effectivePrefix}uploaded_bytes'],
      )!,
      downloadedBytes: attachedDatabase.typeMapping.read(
        DriftSqlType.int,
        data['${effectivePrefix}downloaded_bytes'],
      )!,
      status: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}status'],
      )!,
    );
  }

  @override
  $FileTransfersTable createAlias(String alias) {
    return $FileTransfersTable(attachedDatabase, alias);
  }
}

class FileTransfer extends DataClass implements Insertable<FileTransfer> {
  final String id;
  final int uploadedBytes;
  final int downloadedBytes;
  final String status;
  const FileTransfer({
    required this.id,
    required this.uploadedBytes,
    required this.downloadedBytes,
    required this.status,
  });
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['id'] = Variable<String>(id);
    map['uploaded_bytes'] = Variable<int>(uploadedBytes);
    map['downloaded_bytes'] = Variable<int>(downloadedBytes);
    map['status'] = Variable<String>(status);
    return map;
  }

  FileTransfersCompanion toCompanion(bool nullToAbsent) {
    return FileTransfersCompanion(
      id: Value(id),
      uploadedBytes: Value(uploadedBytes),
      downloadedBytes: Value(downloadedBytes),
      status: Value(status),
    );
  }

  factory FileTransfer.fromJson(
    Map<String, dynamic> json, {
    ValueSerializer? serializer,
  }) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return FileTransfer(
      id: serializer.fromJson<String>(json['id']),
      uploadedBytes: serializer.fromJson<int>(json['uploadedBytes']),
      downloadedBytes: serializer.fromJson<int>(json['downloadedBytes']),
      status: serializer.fromJson<String>(json['status']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'id': serializer.toJson<String>(id),
      'uploadedBytes': serializer.toJson<int>(uploadedBytes),
      'downloadedBytes': serializer.toJson<int>(downloadedBytes),
      'status': serializer.toJson<String>(status),
    };
  }

  FileTransfer copyWith({
    String? id,
    int? uploadedBytes,
    int? downloadedBytes,
    String? status,
  }) => FileTransfer(
    id: id ?? this.id,
    uploadedBytes: uploadedBytes ?? this.uploadedBytes,
    downloadedBytes: downloadedBytes ?? this.downloadedBytes,
    status: status ?? this.status,
  );
  FileTransfer copyWithCompanion(FileTransfersCompanion data) {
    return FileTransfer(
      id: data.id.present ? data.id.value : this.id,
      uploadedBytes: data.uploadedBytes.present
          ? data.uploadedBytes.value
          : this.uploadedBytes,
      downloadedBytes: data.downloadedBytes.present
          ? data.downloadedBytes.value
          : this.downloadedBytes,
      status: data.status.present ? data.status.value : this.status,
    );
  }

  @override
  String toString() {
    return (StringBuffer('FileTransfer(')
          ..write('id: $id, ')
          ..write('uploadedBytes: $uploadedBytes, ')
          ..write('downloadedBytes: $downloadedBytes, ')
          ..write('status: $status')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => Object.hash(id, uploadedBytes, downloadedBytes, status);
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is FileTransfer &&
          other.id == this.id &&
          other.uploadedBytes == this.uploadedBytes &&
          other.downloadedBytes == this.downloadedBytes &&
          other.status == this.status);
}

class FileTransfersCompanion extends UpdateCompanion<FileTransfer> {
  final Value<String> id;
  final Value<int> uploadedBytes;
  final Value<int> downloadedBytes;
  final Value<String> status;
  final Value<int> rowid;
  const FileTransfersCompanion({
    this.id = const Value.absent(),
    this.uploadedBytes = const Value.absent(),
    this.downloadedBytes = const Value.absent(),
    this.status = const Value.absent(),
    this.rowid = const Value.absent(),
  });
  FileTransfersCompanion.insert({
    required String id,
    this.uploadedBytes = const Value.absent(),
    this.downloadedBytes = const Value.absent(),
    required String status,
    this.rowid = const Value.absent(),
  }) : id = Value(id),
       status = Value(status);
  static Insertable<FileTransfer> custom({
    Expression<String>? id,
    Expression<int>? uploadedBytes,
    Expression<int>? downloadedBytes,
    Expression<String>? status,
    Expression<int>? rowid,
  }) {
    return RawValuesInsertable({
      if (id != null) 'id': id,
      if (uploadedBytes != null) 'uploaded_bytes': uploadedBytes,
      if (downloadedBytes != null) 'downloaded_bytes': downloadedBytes,
      if (status != null) 'status': status,
      if (rowid != null) 'rowid': rowid,
    });
  }

  FileTransfersCompanion copyWith({
    Value<String>? id,
    Value<int>? uploadedBytes,
    Value<int>? downloadedBytes,
    Value<String>? status,
    Value<int>? rowid,
  }) {
    return FileTransfersCompanion(
      id: id ?? this.id,
      uploadedBytes: uploadedBytes ?? this.uploadedBytes,
      downloadedBytes: downloadedBytes ?? this.downloadedBytes,
      status: status ?? this.status,
      rowid: rowid ?? this.rowid,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (id.present) {
      map['id'] = Variable<String>(id.value);
    }
    if (uploadedBytes.present) {
      map['uploaded_bytes'] = Variable<int>(uploadedBytes.value);
    }
    if (downloadedBytes.present) {
      map['downloaded_bytes'] = Variable<int>(downloadedBytes.value);
    }
    if (status.present) {
      map['status'] = Variable<String>(status.value);
    }
    if (rowid.present) {
      map['rowid'] = Variable<int>(rowid.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('FileTransfersCompanion(')
          ..write('id: $id, ')
          ..write('uploadedBytes: $uploadedBytes, ')
          ..write('downloadedBytes: $downloadedBytes, ')
          ..write('status: $status, ')
          ..write('rowid: $rowid')
          ..write(')'))
        .toString();
  }
}

abstract class _$ChatDatabase extends GeneratedDatabase {
  _$ChatDatabase(QueryExecutor e) : super(e);
  $ChatDatabaseManager get managers => $ChatDatabaseManager(this);
  late final $MessagesTable messages = $MessagesTable(this);
  late final $RequestsTable requests = $RequestsTable(this);
  late final $FileTransfersTable fileTransfers = $FileTransfersTable(this);
  @override
  Iterable<TableInfo<Table, Object?>> get allTables =>
      allSchemaEntities.whereType<TableInfo<Table, Object?>>();
  @override
  List<DatabaseSchemaEntity> get allSchemaEntities => [
    messages,
    requests,
    fileTransfers,
  ];
}

typedef $$MessagesTableCreateCompanionBuilder =
    MessagesCompanion Function({
      required String messageId,
      required int messageOrder,
      required int accountId,
      required String conversationId,
      required String turnId,
      required String role,
      required String content,
      Value<String> attachments,
      Value<String?> replyToId,
      Value<String?> replyToContent,
      Value<String?> replyToRole,
      required DateTime createdAt,
      Value<int> rowid,
    });
typedef $$MessagesTableUpdateCompanionBuilder =
    MessagesCompanion Function({
      Value<String> messageId,
      Value<int> messageOrder,
      Value<int> accountId,
      Value<String> conversationId,
      Value<String> turnId,
      Value<String> role,
      Value<String> content,
      Value<String> attachments,
      Value<String?> replyToId,
      Value<String?> replyToContent,
      Value<String?> replyToRole,
      Value<DateTime> createdAt,
      Value<int> rowid,
    });

class $$MessagesTableFilterComposer
    extends Composer<_$ChatDatabase, $MessagesTable> {
  $$MessagesTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<String> get messageId => $composableBuilder(
    column: $table.messageId,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<int> get messageOrder => $composableBuilder(
    column: $table.messageOrder,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<int> get accountId => $composableBuilder(
    column: $table.accountId,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<String> get conversationId => $composableBuilder(
    column: $table.conversationId,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<String> get turnId => $composableBuilder(
    column: $table.turnId,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<String> get role => $composableBuilder(
    column: $table.role,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<String> get content => $composableBuilder(
    column: $table.content,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<String> get attachments => $composableBuilder(
    column: $table.attachments,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<String> get replyToId => $composableBuilder(
    column: $table.replyToId,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<String> get replyToContent => $composableBuilder(
    column: $table.replyToContent,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<String> get replyToRole => $composableBuilder(
    column: $table.replyToRole,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<DateTime> get createdAt => $composableBuilder(
    column: $table.createdAt,
    builder: (column) => ColumnFilters(column),
  );
}

class $$MessagesTableOrderingComposer
    extends Composer<_$ChatDatabase, $MessagesTable> {
  $$MessagesTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<String> get messageId => $composableBuilder(
    column: $table.messageId,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<int> get messageOrder => $composableBuilder(
    column: $table.messageOrder,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<int> get accountId => $composableBuilder(
    column: $table.accountId,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<String> get conversationId => $composableBuilder(
    column: $table.conversationId,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<String> get turnId => $composableBuilder(
    column: $table.turnId,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<String> get role => $composableBuilder(
    column: $table.role,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<String> get content => $composableBuilder(
    column: $table.content,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<String> get attachments => $composableBuilder(
    column: $table.attachments,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<String> get replyToId => $composableBuilder(
    column: $table.replyToId,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<String> get replyToContent => $composableBuilder(
    column: $table.replyToContent,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<String> get replyToRole => $composableBuilder(
    column: $table.replyToRole,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<DateTime> get createdAt => $composableBuilder(
    column: $table.createdAt,
    builder: (column) => ColumnOrderings(column),
  );
}

class $$MessagesTableAnnotationComposer
    extends Composer<_$ChatDatabase, $MessagesTable> {
  $$MessagesTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<String> get messageId =>
      $composableBuilder(column: $table.messageId, builder: (column) => column);

  GeneratedColumn<int> get messageOrder => $composableBuilder(
    column: $table.messageOrder,
    builder: (column) => column,
  );

  GeneratedColumn<int> get accountId =>
      $composableBuilder(column: $table.accountId, builder: (column) => column);

  GeneratedColumn<String> get conversationId => $composableBuilder(
    column: $table.conversationId,
    builder: (column) => column,
  );

  GeneratedColumn<String> get turnId =>
      $composableBuilder(column: $table.turnId, builder: (column) => column);

  GeneratedColumn<String> get role =>
      $composableBuilder(column: $table.role, builder: (column) => column);

  GeneratedColumn<String> get content =>
      $composableBuilder(column: $table.content, builder: (column) => column);

  GeneratedColumn<String> get attachments => $composableBuilder(
    column: $table.attachments,
    builder: (column) => column,
  );

  GeneratedColumn<String> get replyToId =>
      $composableBuilder(column: $table.replyToId, builder: (column) => column);

  GeneratedColumn<String> get replyToContent => $composableBuilder(
    column: $table.replyToContent,
    builder: (column) => column,
  );

  GeneratedColumn<String> get replyToRole => $composableBuilder(
    column: $table.replyToRole,
    builder: (column) => column,
  );

  GeneratedColumn<DateTime> get createdAt =>
      $composableBuilder(column: $table.createdAt, builder: (column) => column);
}

class $$MessagesTableTableManager
    extends
        RootTableManager<
          _$ChatDatabase,
          $MessagesTable,
          Message,
          $$MessagesTableFilterComposer,
          $$MessagesTableOrderingComposer,
          $$MessagesTableAnnotationComposer,
          $$MessagesTableCreateCompanionBuilder,
          $$MessagesTableUpdateCompanionBuilder,
          (Message, BaseReferences<_$ChatDatabase, $MessagesTable, Message>),
          Message,
          PrefetchHooks Function()
        > {
  $$MessagesTableTableManager(_$ChatDatabase db, $MessagesTable table)
    : super(
        TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () =>
              $$MessagesTableFilterComposer($db: db, $table: table),
          createOrderingComposer: () =>
              $$MessagesTableOrderingComposer($db: db, $table: table),
          createComputedFieldComposer: () =>
              $$MessagesTableAnnotationComposer($db: db, $table: table),
          updateCompanionCallback:
              ({
                Value<String> messageId = const Value.absent(),
                Value<int> messageOrder = const Value.absent(),
                Value<int> accountId = const Value.absent(),
                Value<String> conversationId = const Value.absent(),
                Value<String> turnId = const Value.absent(),
                Value<String> role = const Value.absent(),
                Value<String> content = const Value.absent(),
                Value<String> attachments = const Value.absent(),
                Value<String?> replyToId = const Value.absent(),
                Value<String?> replyToContent = const Value.absent(),
                Value<String?> replyToRole = const Value.absent(),
                Value<DateTime> createdAt = const Value.absent(),
                Value<int> rowid = const Value.absent(),
              }) => MessagesCompanion(
                messageId: messageId,
                messageOrder: messageOrder,
                accountId: accountId,
                conversationId: conversationId,
                turnId: turnId,
                role: role,
                content: content,
                attachments: attachments,
                replyToId: replyToId,
                replyToContent: replyToContent,
                replyToRole: replyToRole,
                createdAt: createdAt,
                rowid: rowid,
              ),
          createCompanionCallback:
              ({
                required String messageId,
                required int messageOrder,
                required int accountId,
                required String conversationId,
                required String turnId,
                required String role,
                required String content,
                Value<String> attachments = const Value.absent(),
                Value<String?> replyToId = const Value.absent(),
                Value<String?> replyToContent = const Value.absent(),
                Value<String?> replyToRole = const Value.absent(),
                required DateTime createdAt,
                Value<int> rowid = const Value.absent(),
              }) => MessagesCompanion.insert(
                messageId: messageId,
                messageOrder: messageOrder,
                accountId: accountId,
                conversationId: conversationId,
                turnId: turnId,
                role: role,
                content: content,
                attachments: attachments,
                replyToId: replyToId,
                replyToContent: replyToContent,
                replyToRole: replyToRole,
                createdAt: createdAt,
                rowid: rowid,
              ),
          withReferenceMapper: (p0) => p0
              .map((e) => (e.readTable(table), BaseReferences(db, table, e)))
              .toList(),
          prefetchHooksCallback: null,
        ),
      );
}

typedef $$MessagesTableProcessedTableManager =
    ProcessedTableManager<
      _$ChatDatabase,
      $MessagesTable,
      Message,
      $$MessagesTableFilterComposer,
      $$MessagesTableOrderingComposer,
      $$MessagesTableAnnotationComposer,
      $$MessagesTableCreateCompanionBuilder,
      $$MessagesTableUpdateCompanionBuilder,
      (Message, BaseReferences<_$ChatDatabase, $MessagesTable, Message>),
      Message,
      PrefetchHooks Function()
    >;
typedef $$RequestsTableCreateCompanionBuilder =
    RequestsCompanion Function({
      required String requestId,
      required int accountId,
      required String content,
      required String status,
      required DateTime createdAt,
      Value<int> rowid,
    });
typedef $$RequestsTableUpdateCompanionBuilder =
    RequestsCompanion Function({
      Value<String> requestId,
      Value<int> accountId,
      Value<String> content,
      Value<String> status,
      Value<DateTime> createdAt,
      Value<int> rowid,
    });

class $$RequestsTableFilterComposer
    extends Composer<_$ChatDatabase, $RequestsTable> {
  $$RequestsTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<String> get requestId => $composableBuilder(
    column: $table.requestId,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<int> get accountId => $composableBuilder(
    column: $table.accountId,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<String> get content => $composableBuilder(
    column: $table.content,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<String> get status => $composableBuilder(
    column: $table.status,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<DateTime> get createdAt => $composableBuilder(
    column: $table.createdAt,
    builder: (column) => ColumnFilters(column),
  );
}

class $$RequestsTableOrderingComposer
    extends Composer<_$ChatDatabase, $RequestsTable> {
  $$RequestsTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<String> get requestId => $composableBuilder(
    column: $table.requestId,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<int> get accountId => $composableBuilder(
    column: $table.accountId,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<String> get content => $composableBuilder(
    column: $table.content,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<String> get status => $composableBuilder(
    column: $table.status,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<DateTime> get createdAt => $composableBuilder(
    column: $table.createdAt,
    builder: (column) => ColumnOrderings(column),
  );
}

class $$RequestsTableAnnotationComposer
    extends Composer<_$ChatDatabase, $RequestsTable> {
  $$RequestsTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<String> get requestId =>
      $composableBuilder(column: $table.requestId, builder: (column) => column);

  GeneratedColumn<int> get accountId =>
      $composableBuilder(column: $table.accountId, builder: (column) => column);

  GeneratedColumn<String> get content =>
      $composableBuilder(column: $table.content, builder: (column) => column);

  GeneratedColumn<String> get status =>
      $composableBuilder(column: $table.status, builder: (column) => column);

  GeneratedColumn<DateTime> get createdAt =>
      $composableBuilder(column: $table.createdAt, builder: (column) => column);
}

class $$RequestsTableTableManager
    extends
        RootTableManager<
          _$ChatDatabase,
          $RequestsTable,
          Request,
          $$RequestsTableFilterComposer,
          $$RequestsTableOrderingComposer,
          $$RequestsTableAnnotationComposer,
          $$RequestsTableCreateCompanionBuilder,
          $$RequestsTableUpdateCompanionBuilder,
          (Request, BaseReferences<_$ChatDatabase, $RequestsTable, Request>),
          Request,
          PrefetchHooks Function()
        > {
  $$RequestsTableTableManager(_$ChatDatabase db, $RequestsTable table)
    : super(
        TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () =>
              $$RequestsTableFilterComposer($db: db, $table: table),
          createOrderingComposer: () =>
              $$RequestsTableOrderingComposer($db: db, $table: table),
          createComputedFieldComposer: () =>
              $$RequestsTableAnnotationComposer($db: db, $table: table),
          updateCompanionCallback:
              ({
                Value<String> requestId = const Value.absent(),
                Value<int> accountId = const Value.absent(),
                Value<String> content = const Value.absent(),
                Value<String> status = const Value.absent(),
                Value<DateTime> createdAt = const Value.absent(),
                Value<int> rowid = const Value.absent(),
              }) => RequestsCompanion(
                requestId: requestId,
                accountId: accountId,
                content: content,
                status: status,
                createdAt: createdAt,
                rowid: rowid,
              ),
          createCompanionCallback:
              ({
                required String requestId,
                required int accountId,
                required String content,
                required String status,
                required DateTime createdAt,
                Value<int> rowid = const Value.absent(),
              }) => RequestsCompanion.insert(
                requestId: requestId,
                accountId: accountId,
                content: content,
                status: status,
                createdAt: createdAt,
                rowid: rowid,
              ),
          withReferenceMapper: (p0) => p0
              .map((e) => (e.readTable(table), BaseReferences(db, table, e)))
              .toList(),
          prefetchHooksCallback: null,
        ),
      );
}

typedef $$RequestsTableProcessedTableManager =
    ProcessedTableManager<
      _$ChatDatabase,
      $RequestsTable,
      Request,
      $$RequestsTableFilterComposer,
      $$RequestsTableOrderingComposer,
      $$RequestsTableAnnotationComposer,
      $$RequestsTableCreateCompanionBuilder,
      $$RequestsTableUpdateCompanionBuilder,
      (Request, BaseReferences<_$ChatDatabase, $RequestsTable, Request>),
      Request,
      PrefetchHooks Function()
    >;
typedef $$FileTransfersTableCreateCompanionBuilder =
    FileTransfersCompanion Function({
      required String id,
      Value<int> uploadedBytes,
      Value<int> downloadedBytes,
      required String status,
      Value<int> rowid,
    });
typedef $$FileTransfersTableUpdateCompanionBuilder =
    FileTransfersCompanion Function({
      Value<String> id,
      Value<int> uploadedBytes,
      Value<int> downloadedBytes,
      Value<String> status,
      Value<int> rowid,
    });

class $$FileTransfersTableFilterComposer
    extends Composer<_$ChatDatabase, $FileTransfersTable> {
  $$FileTransfersTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<String> get id => $composableBuilder(
    column: $table.id,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<int> get uploadedBytes => $composableBuilder(
    column: $table.uploadedBytes,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<int> get downloadedBytes => $composableBuilder(
    column: $table.downloadedBytes,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<String> get status => $composableBuilder(
    column: $table.status,
    builder: (column) => ColumnFilters(column),
  );
}

class $$FileTransfersTableOrderingComposer
    extends Composer<_$ChatDatabase, $FileTransfersTable> {
  $$FileTransfersTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<String> get id => $composableBuilder(
    column: $table.id,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<int> get uploadedBytes => $composableBuilder(
    column: $table.uploadedBytes,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<int> get downloadedBytes => $composableBuilder(
    column: $table.downloadedBytes,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<String> get status => $composableBuilder(
    column: $table.status,
    builder: (column) => ColumnOrderings(column),
  );
}

class $$FileTransfersTableAnnotationComposer
    extends Composer<_$ChatDatabase, $FileTransfersTable> {
  $$FileTransfersTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<String> get id =>
      $composableBuilder(column: $table.id, builder: (column) => column);

  GeneratedColumn<int> get uploadedBytes => $composableBuilder(
    column: $table.uploadedBytes,
    builder: (column) => column,
  );

  GeneratedColumn<int> get downloadedBytes => $composableBuilder(
    column: $table.downloadedBytes,
    builder: (column) => column,
  );

  GeneratedColumn<String> get status =>
      $composableBuilder(column: $table.status, builder: (column) => column);
}

class $$FileTransfersTableTableManager
    extends
        RootTableManager<
          _$ChatDatabase,
          $FileTransfersTable,
          FileTransfer,
          $$FileTransfersTableFilterComposer,
          $$FileTransfersTableOrderingComposer,
          $$FileTransfersTableAnnotationComposer,
          $$FileTransfersTableCreateCompanionBuilder,
          $$FileTransfersTableUpdateCompanionBuilder,
          (
            FileTransfer,
            BaseReferences<_$ChatDatabase, $FileTransfersTable, FileTransfer>,
          ),
          FileTransfer,
          PrefetchHooks Function()
        > {
  $$FileTransfersTableTableManager(_$ChatDatabase db, $FileTransfersTable table)
    : super(
        TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () =>
              $$FileTransfersTableFilterComposer($db: db, $table: table),
          createOrderingComposer: () =>
              $$FileTransfersTableOrderingComposer($db: db, $table: table),
          createComputedFieldComposer: () =>
              $$FileTransfersTableAnnotationComposer($db: db, $table: table),
          updateCompanionCallback:
              ({
                Value<String> id = const Value.absent(),
                Value<int> uploadedBytes = const Value.absent(),
                Value<int> downloadedBytes = const Value.absent(),
                Value<String> status = const Value.absent(),
                Value<int> rowid = const Value.absent(),
              }) => FileTransfersCompanion(
                id: id,
                uploadedBytes: uploadedBytes,
                downloadedBytes: downloadedBytes,
                status: status,
                rowid: rowid,
              ),
          createCompanionCallback:
              ({
                required String id,
                Value<int> uploadedBytes = const Value.absent(),
                Value<int> downloadedBytes = const Value.absent(),
                required String status,
                Value<int> rowid = const Value.absent(),
              }) => FileTransfersCompanion.insert(
                id: id,
                uploadedBytes: uploadedBytes,
                downloadedBytes: downloadedBytes,
                status: status,
                rowid: rowid,
              ),
          withReferenceMapper: (p0) => p0
              .map((e) => (e.readTable(table), BaseReferences(db, table, e)))
              .toList(),
          prefetchHooksCallback: null,
        ),
      );
}

typedef $$FileTransfersTableProcessedTableManager =
    ProcessedTableManager<
      _$ChatDatabase,
      $FileTransfersTable,
      FileTransfer,
      $$FileTransfersTableFilterComposer,
      $$FileTransfersTableOrderingComposer,
      $$FileTransfersTableAnnotationComposer,
      $$FileTransfersTableCreateCompanionBuilder,
      $$FileTransfersTableUpdateCompanionBuilder,
      (
        FileTransfer,
        BaseReferences<_$ChatDatabase, $FileTransfersTable, FileTransfer>,
      ),
      FileTransfer,
      PrefetchHooks Function()
    >;

class $ChatDatabaseManager {
  final _$ChatDatabase _db;
  $ChatDatabaseManager(this._db);
  $$MessagesTableTableManager get messages =>
      $$MessagesTableTableManager(_db, _db.messages);
  $$RequestsTableTableManager get requests =>
      $$RequestsTableTableManager(_db, _db.requests);
  $$FileTransfersTableTableManager get fileTransfers =>
      $$FileTransfersTableTableManager(_db, _db.fileTransfers);
}
