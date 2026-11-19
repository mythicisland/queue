package client

import (
	"context"
	"crypto/tls"

	"buf.build/gen/go/mythicisland/queue/grpc/go/mythicisland/queue/v1/queuev1grpc"
	"github.com/mythicisland/queue/cli/internal/config"
	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials"
	"google.golang.org/grpc/credentials/insecure"
)

type Client struct {
	conn  *grpc.ClientConn
	Types queuev1grpc.QueueTypeServiceClient
	Data  queuev1grpc.QueueDataServiceClient
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
	)
	if err != nil {
		return nil, err
	}

	return &Client{
		conn:  conn,
		Types: queuev1grpc.NewQueueTypeServiceClient(conn),
		Data:  queuev1grpc.NewQueueDataServiceClient(conn),
	}, nil
}

func (c *Client) Close() error {
	return c.conn.Close()
}

type bearer string

func (b bearer) GetRequestMetadata(context.Context, ...string) (map[string]string, error) {
	return map[string]string{"authorization": "Bearer " + string(b)}, nil
}

func (b bearer) RequireTransportSecurity() bool {
	return false
}
