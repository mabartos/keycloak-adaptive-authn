import type { AuthenticationProviderRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/authenticatorConfigRepresentation";
import {
    Button,
    ButtonVariant,
    Form,
    Modal,
    ModalVariant,
} from "@patternfly/react-core";
import { useEffect, useState } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import {SelectControl, TextControl} from "@keycloak/keycloak-ui-shared";
import { adminClient } from "../../../admin-client";
import { useFetch } from "../../../utils/useFetch";

type AddSubPolicyProps = {
    name: string;
    onConfirm: (flow: Policy) => void;
    onCancel: () => void;
};

export type Policy = {
    name: string;
    description: string;
    type: "basic-flow";
    provider: string;
};

export const AddSubPolicyModal = ({
                                    name,
                                    onConfirm,
                                    onCancel,
                                }: AddSubPolicyProps) => {
    const { t } = useTranslation();
    const form = useForm<Policy>();
    const [formProviders, setFormProviders] =
        useState<AuthenticationProviderRepresentation[]>();

    useFetch(
        () => adminClient.authenticationManagement.getFormProviders(),
        setFormProviders,
        [],
    );

    useEffect(() => {
        if (formProviders?.length === 1) {
            form.setValue("provider", formProviders[0].id!);
        }
    }, [formProviders]);

    return (
        <Modal
            variant={ModalVariant.medium}
            title={t("addStepTo", { name })}
            onClose={onCancel}
            actions={[
                <Button
                    key="add"
                    data-testid="modal-add"
                    type="submit"
                    form="sub-flow-form"
                >
                    {t("add")}
                </Button>,
                <Button
                    key="cancel"
                    data-testid="cancel"
                    variant={ButtonVariant.link}
                    onClick={onCancel}
                >
                    {t("cancel")}
                </Button>,
            ]}
            isOpen
        >
            <Form
                id="sub-flow-form"
                onSubmit={form.handleSubmit(onConfirm)}
                isHorizontal
            >
                <FormProvider {...form}>
                    <TextControl
                        name="name"
                        label={t("name")}
                        labelIcon={t("clientIdHelp")}
                        rules={{ required: { value: true, message: t("required") } }}
                    />
                    <TextControl
                        name="description"
                        label={t("description")}
                        labelIcon={t("flowNameDescriptionHelp")}
                    />
                    {formProviders && formProviders.length > 1 && (
                        <SelectControl
                            name="provider"
                            label={t("provider")}
                            labelIcon={t("authenticationFlowTypeHelp")}
                            options={formProviders.map((provider) => ({
                                key: provider.id!,
                                value: provider.displayName!,
                            }))}
                            controller={{ defaultValue: "" }}
                        />
                    )}
                </FormProvider>
            </Form>
        </Modal>
    );
};
