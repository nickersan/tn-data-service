package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "should return OK 200"
    request{
        method GET()
        url("/noop")
    }
    response {
        status 200
    }
}