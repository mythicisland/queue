package config

import (
	"errors"
	"fmt"
	"os"
	"strconv"
)

type Overrides struct {
	Context string
	URL     string
	Secret  string
}

func (c *Config) Resolve(o Overrides) (Context, error) {
	name := firstSet(o.Context, os.Getenv("QUEUE_CONTEXT"), c.Current)

	var ctx Context
	if name != "" {
		found, ok := c.Contexts[name]
		if !ok {
			return Context{}, fmt.Errorf("context %q does not exist", name)
		}
		ctx = found
	}

	ctx.URL = firstSet(o.URL, os.Getenv("QUEUE_URL"), ctx.URL)
	ctx.Secret = firstSet(o.Secret, os.Getenv("QUEUE_SECRET"), ctx.Secret)
	if tls, err := strconv.ParseBool(os.Getenv("QUEUE_TLS")); err == nil {
		ctx.TLS = tls
	}

	if ctx.URL == "" {
		return Context{}, errors.New("no queue configured, add one with `queue context add` or set QUEUE_URL")
	}
	return ctx, nil
}

func firstSet(values ...string) string {
	for _, value := range values {
		if value != "" {
			return value
		}
	}
	return ""
}
