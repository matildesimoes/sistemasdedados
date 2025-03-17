# Bloco (1)

O bloco tem uma transação assinada pelo buyer, um nounce que é um inteiro gerado aleatoriamente, a hash do bloco anterior, o tempo em que foi criado, a Merkle root hash e ainda a hash do próprio bloco.

# Transação

Tem um buyer, um seller, a informação, o tempo em que foi criada e a assinatura da própria assinada pelo buyer.

A assinatura é feita com todos os parâmetros anteriores excluindo a própria assinatura com SHA256 RSA.

# User

Tem o id e o par de chaves (pública e privada).

Ao criar o user é atributido um id único e criado um par de chaves com RSA-2048.

# Chain

A chain tem uma lista de blocos.

# Blockchain (1)

A blockchain é uma lista de chains.

- Como se cria o bloco?

Temos o bloco genesis que tem como transição uma random string, não tem nounce e o resto é igual aos restantes blocos.

Para criar um bloco, pegamos em todos os parâmetros e fazemos hash deles. Depois esse bloco é mandado para um miner onde vai gerar hashes com nounce aleatórios até encontrar uma hash com uma dificuldade pré definida (proof of work). Se um miner criar o bloco e ele tiver a mesma previous hash que o bloco mais recente da chain, é criada uma nova chain. Essa chain será a copia da anterior, sem o último bloco, com o novo bloco adicionado.

# Merkle Tree (1)

Recebe a chain atual e a transação do bloco que o miner minerou. A árvore combina hashs duas as duas e se o número for ímpar duplica a última hash. No final temos a Merkle root hash.

