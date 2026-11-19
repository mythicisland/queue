package client

import (
	"context"
	"crypto/tls"
	"time"

	"buf.build/gen/go/mythicisland/queue/grpc/go/mythicisland/queue/v1/queuev1grpc"
	"github.com/mythicisland/queue/cli/internal/config"
	"github.com/spf13/cobra"
	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials"
	"google.golang.org/grpc/credentials/insecure"
)

const timeout = 10 * time.Second

type Client struct {
	conn    *grpc.ClientConn
	Context config.Context
	Types   queuev1grpc.QueueTypeServiceClient
	Data    queuev1grpc.QueueDataServiceClient
	Tickets queuev1grpc.TicketServiceClient
}

func Connect(cmd *cobra.Command) (*Client, error) {
	cfg, err := config.Load()
	if err != nil {
		return nil, err
	}

	flags := cmd.Flags()
	var overrides config.Overrides
	overrides.Context, _ = flags.GetString("context")
	overrides.URL, _ = flags.GetString("url")
	overrides.Secret, _ = flags.GetString("secret")

	ctx, err := cfg.Resolve(overrides)
	if err != nil {
		return nil, err
	}
	return New(ctx)
}

func New(ctx config.Context) (*Client, error) {
	transport := insecure.NewCredentials()
	if ctx.TLS {
		transport = credentials.NewTLS(&tls.Config{})
	}

	conn, err := grpc.NewClient(
		ctx.URL,
		grpc.WithTransportCredentials(transport),
		grpc.WithPerRPCCredentials(bearer(ctx.Secret)),
		grpc.WithUnaryInterceptor(intercept(ctx.URL)),
	)
	if err != nil {
		return nil, err
	}

	return &Client{
		conn:    conn,
		Context: ctx,
		Types:   queuev1grpc.NewQueueTypeServiceClient(conn),
		Data:    queuev1grpc.NewQueueDataServiceClient(conn),
		Tickets: queuev1grpc.NewTicketServiceClient(conn),
	}, nil
}

func (c *Client) Close() error {
	return c.conn.Close()
}

func intercept(url string) grpc.UnaryClientInterceptor {
	return func(ctx context.Context, method string, req, reply any, cc *grpc.ClientConn, invoker grpc.UnaryInvoker, opts ...grpc.CallOption) error {
		if _, ok := ctx.Deadline(); !ok {
			var cancel context.CancelFunc
			ctx, cancel = context.WithTimeout(ctx, timeout)
			defer cancel()
		}
		return readable(invoker(ctx, method, req, reply, cc, opts...), url)
	}
}

type bearer string

func (b bearer) GetRequestMetadata(context.Context, ...string) (map[string]string, error) {
	return map[string]string{"authorization": "Bearer " + string(b)}, nil
}

func (b bearer) RequireTransportSecurity() bool {
	return false
}
