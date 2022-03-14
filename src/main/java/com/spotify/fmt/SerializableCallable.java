package com.spotify.fmt;

import java.io.Serializable;
import java.util.concurrent.Callable;

interface SerializableCallable<T> extends Callable<T>, Serializable {}
